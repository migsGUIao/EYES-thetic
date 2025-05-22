import tensorflow as tf

# Load your .h5 model
model = tf.keras.models.load_model('resnet_fashion_classifier.h5')

# Convert the Keras model to TensorFlow Lite
converter = tf.lite.TFLiteConverter.from_keras_model(model)

# Optional: Apply optimizations (e.g., quantization for smaller size/faster inference)
# converter.optimizations = [tf.lite.Optimize.DEFAULT]
# converter.target_spec.supported_types = [tf.float16] # For float16 quantization

tflite_model = converter.convert()

# Save the .tflite model
with open('resnet_fashion_classifier.tflite', 'wb') as f:
    f.write(tflite_model)

print("Model converted to resnet_fashion_classifier.tflite")