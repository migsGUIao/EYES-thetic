import sys
import numpy as np
from PIL import Image
import tensorflow as tf

# Load the saved model
model = tf.keras.models.load_model('resnet_fashion_classifier.h5')

def preprocess_image(image_path):
    img = Image.open(image_path).resize((224, 224))
    img_array = np.array(img) / 255.0
    img_array = np.expand_dims(img_array, axis=0)
    return img_array

def predict_category(image_path):
    img_array = preprocess_image(image_path)
    prediction = model.predict(img_array)
    # Assume class indices: 0 => top, 1 => bottom
    if np.argmax(prediction) == 0:
        return "top"
    else:
        return "bottom"

if __name__ == "__main__":
    image_path = sys.argv[1]
    print(predict_category(image_path))