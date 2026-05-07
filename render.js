const puppeteer = require('puppeteer');
const path = require('path');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.setViewport({ width: 1024, height: 500 });
  const fileUrl = 'file://' + path.resolve('mockup/demo.html');
  await page.goto(fileUrl, { waitUntil: 'networkidle0' });
  await page.screenshot({ path: 'mockup/feature_graphic.png', clip: { x: 0, y: 0, width: 1024, height: 500 } });
  await browser.close();
})();
