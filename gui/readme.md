# SoftVC VITS Singing Voice Conversion Java Swing GUI

## Overview
This project was designed to alleviate the difficulties & confusions for non CS-related people by giving them a **Graphical User Interface** & **Embedded Utilities** instead of only a command line to operate on So-VITS-SVC program.

### **Requirements**
- NVIDIA Graphic Card(s) with CUDA
- Windows OS-dependent
- **Java Development Kit**: "https://www.oracle.com/java/technologies/downloads/" or Java Runtime Environment (deprecated)
- Dependencies needed but NOT included in this Repo: (but **will be packed into Release**)
  - .\workenv\ (Portable Working-Environment)
  - .\so-vits-svc-4.1-Stable\pretrain\ (Pretrained Models)
  - .\so-vits-svc-4.1-Stable\logs\44k\ (So-VITS-SVC Training Logs & Base Models)

### **User Stories**
- Be able to Prepare dataset_raw, Preprocess dataset, Train models & Infer vocals with crucial user-customized configuration overwrite.
- Be able to Monitor GPU Status/Usage.
- Be able to get real-time responsive feedback from embedded Console area.
- Be able to clear Console screen by right click on Console area.
- Be able to interact with GUI while Background-Tasks are running, and GUI quickly responds.
- Be able to schedule Tasks and execute them later by the time-order as the order they were added.
- When invalid inputs happen accidentally, there should be neither fatal error nor file-system destruction occurs, and program should immediately restore into a Valid/Safe STATE.

### **References**
- So-VITS-SVC main project repo: "https://github.com/svc-develop-team/so-vits-svc"
- Audio Slicer repo: "https://github.com/openvpi/audio-slicer"
- "GUI-Icon.png", retrieved from "https://avatars.githubusercontent.com/u/127122328?s=400&u=5395a98a4f945a3a50cb0cc96c2747505d190dbc&v=4"
