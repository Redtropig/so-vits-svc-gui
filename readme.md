# SoftVC VITS Singing Voice Conversion Java Swing GUI

## Overview
This project was designed to alleviate the difficulties & confusion for non-CS-related people by giving them a **Graphical User Interface** & **Embedded Utilities** instead of only a command line to operate on the So-VITS-SVC program.

### **Requirements**
- NVIDIA Graphic Card(s) with CUDA
- Windows OS-dependent
- [**Java Development Kit**](https://www.oracle.com/java/technologies/downloads/) or Java Runtime Environment (deprecated)
- Dependencies needed but NOT included in this Repo: (**have been packed into _[Releases](https://github.com/Redtropig/so-vits-svc-gui/releases)_**)
    - `.\workenv\` (Portable Working-Environment)
    - `.\so-vits-svc-4.1-Stable\pretrain\` (Pretrained Models)
    - `.\so-vits-svc-4.1-Stable\logs\44k\` (So-VITS-SVC Training Logs & Base Models)

### **User Stories**
- Be able to Prepare dataset_raw, Preprocess dataset, Train models & Infer vocals with user-customized configuration overwrite.
- Be able to Monitor GPU Status/Usage.
- Be able to Pick one Graphic Card for training & inference, if there are multiple NVIDIA GPUs with CUDA.
- Be able to get real-time responsive feedback from the embedded Console area.
- Be able to clear the Console screen by right-clicking on the Console area.
- Be able to interact with GUI while Background Tasks are running, and GUI quickly responds.
- Be able to schedule Tasks and execute them later by the time order as the order they were added.
- Be able to process Multiple Files in a batch at one time.
- Be able to connect to [so-vits-svc-server](https://github.com/Redtropig/so-vits-svc-server) as Client.
- When invalid inputs happen accidentally, there should be neither fatal error nor file-system destruction occurs, and the program should show its robustness that immediately restores into a Valid/Safe STATE.

### **References**
- So-VITS-SVC main project repo: [so-vits-svc](https://github.com/svc-develop-team/so-vits-svc)
- Audio Slicer repo: [audio-slicer](https://github.com/openvpi/audio-slicer)
- [GUI-Icon.png](https://avatars.githubusercontent.com/u/127122328?s=400&u=5395a98a4f945a3a50cb0cc96c2747505d190dbc&v=4)
