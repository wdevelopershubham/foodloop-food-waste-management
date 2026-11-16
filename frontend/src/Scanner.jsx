import { useEffect, useState } from 'react';
import { Html5Qrcode } from 'html5-qrcode';

/* Opens the camera, reads one QR code, hands the text to onScan. Needs https or localhost. */
export default function Scanner({ onScan, onClose }) {
  const [err, setErr] = useState('');
  useEffect(() => {
    const qr = new Html5Qrcode('qr-reader');
    let done = false;
    qr.start({ facingMode: 'environment' }, { fps: 10, qrbox: 220 },
      (text) => { if (done) return; done = true; qr.stop().catch(() => {}).finally(() => onScan(text)); },
      () => {})
      .catch(() => setErr('Camera not available. Allow camera access, or paste the code instead.'));
    return () => { if (qr.isScanning) qr.stop().catch(() => {}); };
  }, []); // eslint-disable-line
  return (
    <div className="qr" style={{ display: 'block' }}>
      <div id="qr-reader" style={{ width: '100%', maxWidth: 320 }} />
      {err && <p className="err">{err}</p>}
      <p><button className="ghost" onClick={onClose}>Close camera</button></p>
    </div>
  );
}
