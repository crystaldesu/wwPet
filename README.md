# wwPet

轻量、可自定义的 Windows 桌宠

## 功能

- 鼠标悬停：显示名称、状态、等级、专注时长、陪伴天数
- 处于专注状态时：额外显示本轮专注计时
- 开启定时专注时：额外显示剩余专注时间
- 左键单击：与桌宠互动
- 左键拖拽：移动桌宠，靠近任务栏时自动吸附
- 右键桌宠：打开桌宠菜单

## 自定义资源

如果没有添加美术资源，程序会自动绘制桌宠。

可将自定义图片放入：

- `data/assets/rest.png`
- `data/assets/focus.png`

同时也支持同名的 `gif`、`jpg`、`jpeg`。

## 自定义字体

通过修改 `data/settings.json` 控制字体

说明：

- `fontFamily`：使用系统已安装字体
- `fontFile`：可填写相对项目根目录的字体文件路径，例如 `data/fonts/MyFont.ttf`
- 当 `fontFile` 有效时，会优先使用字体文件
- 修改配置后重新启动 `wwPet` 即可生效

## 编译

运行 `run.bat`

## 打包为可执行文件

运行 `package.bat`

打包完成后，可直接运行：

```bat
dist\wwPet\wwPet.exe
```

