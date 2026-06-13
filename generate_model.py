import tensorflow as tf

# Load the pre-trained MobileNetV2 model without the top classification layer
model = tf.keras.applications.MobileNetV2(weights='imagenet', include_top=False, input_shape=(224, 224, 3), pooling='avg')

# Convert the model to TensorFlow Lite
converter = tf.lite.TFLiteConverter.from_keras_model(model)

# For better performance on mobile, we can optionally quantize it, but let's stick to float32 for best accuracy first
tflite_model = converter.convert()

# Save the model
with open('app/src/main/assets/mobilenet_fv.tflite', 'wb') as f:
    f.write(tflite_model)

print("TFLite Feature Vector model generated successfully!")
