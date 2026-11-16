import { useEffect } from 'react';
import { MapContainer, TileLayer, CircleMarker, Popup, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';

const COLOR = { CREATED: '#c47f00', NGO_ACCEPTED: '#0e6b4d', VOLUNTEER_ASSIGNED: '#0e6b4d', PICKED_UP: '#0e6b4d',
                DELIVERED: '#094a36', EXPIRED: '#b3372e', CANCELLED: '#b3372e' };

function Fit({ pts }) {
  const map = useMap();
  useEffect(() => { if (pts.length) map.fitBounds(pts, { padding: [40, 40], maxZoom: 15 }); }, [pts.length]); // eslint-disable-line
  return null;
}

export default function MapView({ items }) {
  const withPos = items.filter((d) => d.latitude != null && d.longitude != null);
  if (withPos.length === 0)
    return <div className="card empty">No donations with a location yet. Add latitude and longitude to your profile so pickups appear on the map.</div>;
  const pts = withPos.map((d) => [d.latitude, d.longitude]);
  return (
    <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
      <MapContainer center={pts[0]} zoom={12} style={{ height: 420, width: '100%' }}>
        <TileLayer attribution="&copy; OpenStreetMap contributors" url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        <Fit pts={pts} />
        {withPos.map((d) => (
          <CircleMarker key={d.id} center={[d.latitude, d.longitude]} radius={d.urgent ? 13 : 9}
            pathOptions={{ color: d.urgent ? '#b3372e' : COLOR[d.status], fillOpacity: 0.7 }}>
            <Popup>
              <b>{d.foodCategory}, {d.quantityKg} kg</b><br />{d.donorName}<br />
              {d.status.replaceAll('_', ' ').toLowerCase()}{d.urgent ? ' (urgent)' : ''}<br />
              Best before {new Date(d.expiresAt).toLocaleString()}
            </Popup>
          </CircleMarker>
        ))}
      </MapContainer>
    </div>
  );
}
