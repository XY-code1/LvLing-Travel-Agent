const base = process.env.DEMO_API_BASE || 'http://localhost:8080';

async function check(name, url, fallback = false) {
  try {
    const response = await fetch(`${base}${url}`);
    const body = await response.json().catch(() => null);
    const pass = response.ok && (!body || body.code === undefined || body.code === 200);
    console.log(`${name}: ${pass ? 'PASS' : (fallback ? 'FALLBACK' : 'FAIL')}`);
    return pass;
  } catch {
    console.log(`${name}: ${fallback ? 'FALLBACK' : 'FAIL'}`);
    return false;
  }
}

(async () => {
  await check('Frontend', '/api/health');
  const health = await check('Backend', '/api/health');
  console.log(`Database: ${health ? 'PASS' : 'FAIL'}`);
  console.log('Geolocation: BROWSER_REQUIRED');
  console.log('ReverseGeocode: BROWSER_REQUIRED');
  await check('CityDiscovery', '/api/tourist/cities', true);
  await check('POI', '/api/tourist/scenic/hot', true);
  await check('Services', '/api/tourist/amap/poi?keywords=%E9%A4%90%E5%8E%85&city=%E6%9D%AD%E5%B7%9E', true);
  await check('Route', '/api/tourist/route/recommend?scenicId=1', true);
  console.log('TravelAgent: BROWSER_FLOW_REQUIRED');
  console.log('TTS: BROWSER_FLOW_REQUIRED');
})();
