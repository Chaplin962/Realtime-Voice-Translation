Overview

This project is a fork of niedev/RTranslator[https://github.com/niedev/RTranslator]
 that extends the original app with VoIP support and a room-based conversation model, making real-time, multi-user conversations more practical and scalable.

The goal is to move beyond one-off translations and enable more natural, shared communication experiences—especially for voice-based interactions.

Getting Started

Before building the APK, complete the following setup steps:

Rebuild ONNX models
Run the script below to merge the split ONNX model files:

app/src/main/assets/rebuild_models.sh


Configure Firebase
Add your Firebase configuration file at:

app/google-services.json


Follow the official Firebase Android setup guide if needed:
[https://firebase.google.com/docs/android/setup]
