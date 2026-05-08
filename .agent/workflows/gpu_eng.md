---
description: GPU SYSTEMS ENGINEER
---

# WORKFLOW: GPU SYSTEMS ENGINEER
**Trigger Command:** `/gpu_eng`
**Tech Stack:** Nvidia CUDA | Apple Metal

1. **Detection**: Check the deployment target environment.
   - Server? -> Generate Python + CUDA kernels.
   - Apple Device? -> Generate Swift/Obj-C + Metal Performance Shaders.
2. **Optimization**: Focus purely on parallelization and memory management.