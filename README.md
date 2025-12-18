# AutoGLM-Android-Termux
## 项目介绍

本项目支持通过语音识别传输并执行命令，前台悬浮条长按可快速启动语音识别功能，无需频繁切换应用前后台。项目采用分层部署架构：AutoGLM 运行于 Termux-Ubuntu 环境，AutoGLM-Controller 应用通过共享文件实现与 Termux 的命令传输。

## 部署步骤

**AutoGLM在Termux运行**

1、下载Termux

首先在安卓设备上安装 Termux 应用。

2、设置Termux访问手机存储

执行以下命令并授予存储访问权限，Termux 主目录将生成 `Storage` 子目录，共享文件最终存放于 `/data/data/com.termux/files/home/storage/shared/UbuntuAndroid`。

```bash
termux-setup-storage
```

3、Termux安装Ubuntu

通过 `proot-distro` 工具安装 Ubuntu 环境，步骤如下：

```bash
#- 安装proot-distro
pkg install proot-distro -y
#- 查看有哪些系统
proot-distro list
#- 安装ubuntu系统
proot-distro install ubuntu
#- 登录ubuntu系统
proot-distro login ubuntu
```

提示：若直接在 Termux 中运行 AutoGLM 项目，OpenAI 的 jiter 依赖需通过 Rust 环境编译，过程中易出现报错。因此建议在上述 Ubuntu 环境中部署运行。

4、安装Conda

执行以下命令下载并安装 Miniconda：

```bash
#- 下载miniconda安装脚本
wget -v https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-aarch64.sh
#- 运行安装脚本
bash Miniconda3-latest-Linux-aarch64.sh
```

5、AutoGLM项目部署

参考官方部署教程：[zai-org/Open-AutoGLM: An Open Phone Agent Model & Framework. Unlocking the AI Phone for Everyone](https://github.com/zai-org/Open-AutoGLM)

6、adb连接

需通过无线调试实现 adb 连接，步骤如下：

1. 进入手机「设置 > 开发者选项」，启用「无线调试」；

2. 选择「配对码授权」，通过以下命令完成配对：

   ```bash
   adb pair IP地址和端口
   ```

3. 完成配对后，执行以下命令完成adb连接

   ```
   adb connect IP地址
   ```

7、启动AutoGLM

支持两种运行方式：使用智谱官方 API 接口，或本地部署模型。启动时需添加 `--android-mode` 参数以启用与 AutoGLM-Controller 应用的通信：

```bash
python main.py --base-url https://open.bigmodel.cn/api/paas/v4 --model "autoglm-phone" --apikey "" --android-mode
```

**AutoGLM-Controller应用**

**1. 讯飞语音识别 SDK 配置**

项目依赖讯飞语音识别 SDK，需完成以下操作：

- 在 `SpeechApplication` 中填写个人 appID（注意保留符号 `"="`）；
- 上传对应 SDK 文件（包括`Msc.jar`及`Jnilibs`目录下的`.so`文件）。

![image-20251218101510245](asset/image-20251218101510245.png)

**2. 运行环境说明**

应用通过 adb 实现控制功能，需依赖手机的无线调试功能，因此仅支持在 WiFi 环境下运行，但不受 adb 连接范围限制。

**3. 演示视频**

<video src="asset/5cf9ea8dd895712408a136085d835178.mp4"></video>

