# MODNet Model

Place your `modnet.onnx` model file in this directory.

## How to get the model

1. Download MODNet from: https://github.com/ZHKKKe/MODNet
2. Export to ONNX format using their provided script
3. Rename to `modnet.onnx` and place here

Or use a pre-converted model:
- modnet_photographic_portrait_matting.onnx (~25MB)

The model input should be:
- Shape: [1, 3, 512, 512]
- Format: CHW, normalized with ImageNet mean/std

The model output should be:
- Shape: [1, 1, 512, 512]
- Alpha matte values between 0 and 1
