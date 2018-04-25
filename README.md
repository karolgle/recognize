# Recognize

Simple app that show usage of [Google Vision API](https://cloud.google.com/vision/) and [Amazon Rekognition](https://aws.amazon.com/rekognition/).
Following features are used: 
- face detection(incl. facial expression), 
- faces comparison, 
- id documents detection(French specifically), 
- passport document detection, 
- machine readable zone reading.

## Install

Google and AWS credentials need to be configured see [Wiki](https://github.com/karolgle/recognize/wiki). 


## Development server
Run `gradlew bootRun` to start backend server.
Run `ng serve` or `npe start` for a dev server. Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.
