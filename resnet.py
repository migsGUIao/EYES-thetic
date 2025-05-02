import os
import shutil
import pandas as pd
import numpy as np
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from tensorflow.keras.applications import ResNet50
from tensorflow.keras.layers import Dense, GlobalAveragePooling2D, Dropout
from tensorflow.keras.models import Model
import tensorflow as tf
from sklearn.metrics import classification_report, precision_score, recall_score, f1_score


# Define file paths
csv_path = 'styles.csv'            # Your original CSV file with metadata
images_dir = 'images'              
# Folder where all images are stored (e.g., "12345.jpg")
output_dir = 'data'                # Directory where images will be organized

topwear_dir = os.path.join(output_dir, 'topwear')
bottomwear_dir = os.path.join(output_dir, 'bottomwear')

os.makedirs(topwear_dir, exist_ok=True)
os.makedirs(bottomwear_dir, exist_ok=True)

df = pd.read_csv(csv_path)
df = df[df['gender'].isin(['Men', 'Women'])].copy()

df.loc[
    (df['gender'] == 'Men') & (df['productDisplayName'].str.contains("Kids", case=False, na=False)),
    'gender'
] = 'Boys'

df = df[df['subCategory'].isin(['Topwear', 'Bottomwear'])]

print("Number of items after filtering:", len(df))
print("Unique genders:", df['gender'].unique())

for index, row in df.iterrows():
    image_id = str(row['id']).strip()
    sub_cat = row['subCategory'].strip().lower()  # expecting 'Topwear' or 'Bottomwear'
    src_image = os.path.join(images_dir, f"{image_id}.jpg")  # adjust extension if necessary

    if os.path.exists(src_image):
        if sub_cat == 'topwear':
            dst_image = os.path.join(topwear_dir, f"{image_id}.jpg")
        elif sub_cat == 'bottomwear':
            dst_image = os.path.join(bottomwear_dir, f"{image_id}.jpg")
        else:
            continue  # Skip if subCategory does not match
        shutil.copy(src_image, dst_image)
    else:
        print(f"Image not found: {src_image}")

print("Dataset preparation complete. Images are organized in the 'data' directory.")

# --------------------------
# STEP 2: Data Generators
# --------------------------

IMG_SIZE = (224, 224)
BATCH_SIZE = 32

datagen = ImageDataGenerator(
    rescale=1./255,
    validation_split=0.2,  # 20% of data for validation
    horizontal_flip=True,
    rotation_range=20,
    zoom_range=0.2
)

train_generator = datagen.flow_from_directory(
    output_dir,             # This directory now has subdirectories 'topwear' and 'bottomwear'
    target_size=IMG_SIZE,
    batch_size=BATCH_SIZE,
    class_mode='categorical',
    subset='training'
)

validation_generator = datagen.flow_from_directory(
    output_dir,
    target_size=IMG_SIZE,
    batch_size=BATCH_SIZE,
    class_mode='categorical',
    subset='validation'
)

# --------------------------
# STEP 3: Build the Model
# --------------------------

# Load pre-trained ResNet50 without its top layers
base_model = ResNet50(weights='imagenet', include_top=False, input_shape=(IMG_SIZE[0], IMG_SIZE[1], 3))
# Freeze the base model layers
for layer in base_model.layers:
    layer.trainable = False

# Add custom classifier on top
x = base_model.output
x = GlobalAveragePooling2D()(x)
x = Dropout(0.5)(x)
x = Dense(128, activation='relu')(x)
predictions = Dense(2, activation='softmax')(x)  # Two classes: topwear and bottomwear

model = Model(inputs=base_model.input, outputs=predictions)
model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])

model.summary()

# --------------------------
# STEP 4: Train the Model
# --------------------------

EPOCHS = 10  # Adjust as needed

history = model.fit(
    train_generator,
    validation_data=validation_generator,
    epochs=EPOCHS
)

# ========= Evaluate Using scikit-learn Metrics =========
# Reset the validation generator and predict on validation data
validation_generator.reset()
val_steps = validation_generator.samples // validation_generator.batch_size
predictions_np = model.predict(validation_generator, steps=val_steps + 1)
predicted_classes = np.argmax(predictions_np, axis=1)

# Ground-truth labels
true_classes = validation_generator.classes
class_labels = list(validation_generator.class_indices.keys())

print("\nClassification Report:")
print(classification_report(true_classes, predicted_classes, target_names=class_labels))

print("Precision:", precision_score(true_classes, predicted_classes, average='weighted'))
print("Recall:", recall_score(true_classes, predicted_classes, average='weighted'))
print("F1 Score:", f1_score(true_classes, predicted_classes, average='weighted'))

# --------------------------
# STEP 5: Save the Model
# --------------------------

model.save("resnet_fashion_classifier.h5")
print("Model trained and saved as resnet_fashion_classifier.h5")