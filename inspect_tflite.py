import tensorflow as tf

# Load the TFLite model
interpreter = tf.lite.Interpreter(model_path="app/src/main/assets/mobilenet.tflite")
interpreter.allocate_tensors()

# Get input and output tensors
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print("Inputs:")
for detail in input_details:
    print(f"Name: {detail['name']}, Shape: {detail['shape']}, Type: {detail['dtype']}")

print("\nOutputs:")
for detail in output_details:
    print(f"Name: {detail['name']}, Shape: {detail['shape']}, Type: {detail['dtype']}")
