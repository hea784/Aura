/*
 * Aura 防指纹脚本 — 降低网站对设备的可识别性（反跟踪）。
 * 仅改写/加噪用于指纹的只读属性，不读取 Cookie/存储/用户输入，不破坏页面功能。
 */
(function () {
  'use strict';
  if (window.__auraAntiFp) return;
  window.__auraAntiFp = true;

  function def(obj, prop, value) {
    try { Object.defineProperty(obj, prop, { get: function () { return value; }, configurable: true }); } catch (e) {}
  }

  // 收敛硬件/内存/核心数等高精度指纹源到常见值
  try {
    def(navigator, 'hardwareConcurrency', 4);
    def(navigator, 'deviceMemory', 4);
    def(navigator, 'plugins', []);
    def(navigator, 'mimeTypes', []);
    def(navigator, 'languages', ['zh-CN', 'zh']);
    def(navigator, 'doNotTrack', '1');
  } catch (e) {}

  // 屏蔽电池状态 API（强指纹源）
  try { if (navigator.getBattery) navigator.getBattery = function () { return Promise.reject(new Error('disabled')); }; } catch (e) {}

  // Canvas 加噪：每次读取引入极小随机扰动，破坏稳定 canvas 指纹
  try {
    var origGetImageData = CanvasRenderingContext2D.prototype.getImageData;
    CanvasRenderingContext2D.prototype.getImageData = function () {
      var data = origGetImageData.apply(this, arguments);
      try {
        for (var i = 0; i < data.data.length; i += 97) {
          data.data[i] = data.data[i] ^ 1;
        }
      } catch (e) {}
      return data;
    };
    var origToDataURL = HTMLCanvasElement.prototype.toDataURL;
    HTMLCanvasElement.prototype.toDataURL = function () {
      try {
        var ctx = this.getContext('2d');
        if (ctx) {
          var d = ctx.getImageData(0, 0, Math.min(8, this.width), Math.min(8, this.height));
          ctx.putImageData(d, 0, 0);
        }
      } catch (e) {}
      return origToDataURL.apply(this, arguments);
    };
  } catch (e) {}

  // WebGL 渲染器信息收敛（避免暴露精确 GPU 型号）
  try {
    var getParam = WebGLRenderingContext.prototype.getParameter;
    WebGLRenderingContext.prototype.getParameter = function (p) {
      if (p === 37445) return 'Aura';           // UNMASKED_VENDOR_WEBGL
      if (p === 37446) return 'Aura Graphics'; // UNMASKED_RENDERER_WEBGL
      return getParam.apply(this, arguments);
    };
  } catch (e) {}

  // 收敛屏幕/时区偏移的精确指纹（保留可用性）
  try {
    def(screen, 'colorDepth', 24);
    def(screen, 'pixelDepth', 24);
  } catch (e) {}
})();
