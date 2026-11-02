# Quark SELinux consolidation through stage 35

Branch: A11+KSU

Runtime validated through stage 35 on LineageOS 18.1 user/Enforcing, including KernelSU and the overlay.

This branch consolidates the source and camera-blob changes used by the incremental SELinux validation. Use the matching local_manifests branch to select all policy repositories together. No SELINUX_IGNORE_NEVERALLOWS bypass is required for the validated Android 11 policy. The custom legacy HAL1 camera ownership contract is not a claim of stock AOSP policy/CTS conformance.

Pending investigations: thermal-engine DAC audit records, camera_prop reads and third-party diagnostic apps such as AIDA64. Normal observed temperature does not establish that every control path is validated. The integrated overlay has read-only metric access; no broad DAC capabilities were restored.

Camera blobs retain file sizes while correcting AIS relocation and migrating legacy camera socket/album literals. The daemon entrypoint and migration source live in the device repository.

Modified ELF SHA-256:

- quark/proprietary/lib/libAlAisLib.so: e7b9323a5bd4a85a2cb54db5313060d080498e5131da62d922407a0f6676e799
- quark/proprietary/vendor/bin/mm-qcamera-daemon: 190361253f1bcc7e326804d49d5251782e0a3d2bcef00d7b46b520cbfb496a39
- quark/proprietary/lib/libmmcamera_interface.so: 2e8befeb02694477a582472d9844c252e2b428100f2500c504bc2ba24ef8e8be
- quark/proprietary/vendor/lib/libmmcamera_imglib.so: 3d193ef5c1dd97641ed06426e888c7aee631296924a2b3b9e9db9f6352401d9a
