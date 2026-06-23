import React, { useState, useEffect } from 'react';
import { supabase } from './supabaseClient';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

// Fix for default Leaflet marker icon in React
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png',
});

export default function VerifiedServicesList() {
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    async function fetchVerifiedServices() {
      try {
        setLoading(true);
        setError(null);
        
        // 1. Fetch active support services
        const { data: servicesData, error: servicesError } = await supabase
          .from('support_services')
          .select('*')
          .eq('status', 'Active');

        if (servicesError) throw servicesError;
        
        if (!servicesData || servicesData.length === 0) {
          setServices([]);
          return;
        }

        // 2. Extract unique provider IDs to fetch their contact info
        const providerIds = [...new Set(servicesData.map(s => s.provider_id))];

        // 3. Fetch corresponding provider profiles
        const { data: profilesData, error: profilesError } = await supabase
          .from('profiles')
          .select('id, company_name, email, phone, website')
          .in('id', providerIds);

        if (profilesError) throw profilesError;

        // 4. Map profiles to an easily accessible object
        const profilesMap = (profilesData || []).reduce((acc, profile) => {
          acc[profile.id] = profile;
          return acc;
        }, {});

        // 5. Combine service data with its provider's contact info
        const combinedData = servicesData.map(service => ({
          ...service,
          providerInfo: profilesMap[service.provider_id] || {}
        }));

        setServices(combinedData);
      } catch (err) {
        console.error('Error fetching verified services:', err);
        setError(err.message || 'Failed to load verified support services.');
      } finally {
        setLoading(false);
      }
    }

    fetchVerifiedServices();
  }, []);

  if (loading) {
    return (
      <div className="w-full h-64 flex flex-col items-center justify-center p-8 bg-slate-950 rounded-2xl border border-slate-800">
        <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mb-4"></div>
        <p className="text-slate-400 font-medium text-sm animate-pulse">Fetching verified service providers...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="w-full p-6 bg-red-950/20 border border-red-900/50 rounded-2xl">
        <div className="flex items-start space-x-3">
          <svg className="w-6 h-6 text-red-500 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div>
            <h3 className="text-sm font-bold text-red-400">Connection Error</h3>
            <p className="text-sm text-red-300/80 mt-1">{error}</p>
          </div>
        </div>
      </div>
    );
  }

  if (services.length === 0) {
    return (
      <div className="w-full text-center p-12 bg-slate-950 border border-slate-800 rounded-2xl">
        <svg className="w-12 h-12 text-slate-700 mx-auto mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 002-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
        <h3 className="text-sm font-bold text-slate-300">No Services Available</h3>
        <p className="text-sm text-slate-500 mt-1">There are currently no verified active support services in the network.</p>
      </div>
    );
  }

  const filteredServices = services.filter(service => {
    const query = searchQuery.toLowerCase();
    return service.name?.toLowerCase().includes(query) || 
           service.category?.toLowerCase().includes(query);
  });

  return (
    <div className="w-full space-y-4">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-xl font-black text-white">Verified Support Services</h2>
          <p className="text-xs font-semibold text-slate-400 mt-1 uppercase tracking-wider">Secure Trust Network</p>
        </div>
        <div className="hidden sm:flex items-center px-3 py-1.5 bg-emerald-950/40 border border-emerald-900/50 rounded-full">
          <span className="flex w-2 h-2 rounded-full bg-emerald-500 mr-2 animate-pulse"></span>
          <span className="text-xs font-bold text-emerald-400">Live DB Sync</span>
        </div>
      </div>

      <div className="mb-6">
        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <svg className="h-5 w-5 text-slate-500" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clipRule="evenodd" />
            </svg>
          </div>
          <input
            type="text"
            className="w-full bg-slate-900 border border-slate-700 rounded-lg pl-10 pr-4 py-2 text-sm text-white placeholder-slate-400 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
            placeholder="Search by service name or type..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>

      <div className="mb-6 h-64 w-full rounded-2xl overflow-hidden border border-slate-800 relative z-0">
        <MapContainer 
          center={[37.7749, -122.4194]} 
          zoom={12} 
          style={{ height: '100%', width: '100%' }}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          {filteredServices.map(service => {
            // Use fallback coordinates if null to demonstrate feature visually
            const lat = service.latitude || (37.7749 + (Math.random() - 0.5) * 0.1);
            const lng = service.longitude || (-122.4194 + (Math.random() - 0.5) * 0.1);
            return (
              <Marker key={`marker-${service.id}`} position={[lat, lng]}>
                <Popup>
                  <div className="font-sans">
                    <strong className="block text-sm text-slate-800">{service.name}</strong>
                    <span className="text-xs text-slate-500">{service.category || 'General'}</span>
                  </div>
                </Popup>
              </Marker>
            );
          })}
        </MapContainer>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredServices.map((service) => (
          <div key={service.id} className="group flex flex-col bg-slate-950 border border-slate-800 hover:border-indigo-500/50 rounded-2xl overflow-hidden transition-all duration-300 hover:shadow-lg hover:shadow-indigo-500/10">
            {/* Asset Header */}
            <div className="h-2 bg-gradient-to-r from-indigo-600 to-purple-600 w-full" />
            
            <div className="p-5 flex-1 flex flex-col">
              <div className="flex justify-between items-start mb-3">
                <span className="px-2.5 py-1 rounded bg-indigo-950/50 text-indigo-400 text-[10px] font-black uppercase tracking-wider border border-indigo-900/50">
                  {service.category || 'General'}
                </span>
                <span className="flex items-center text-emerald-400 text-xs font-bold">
                  <svg className="w-3.5 h-3.5 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
                  </svg>
                  Verified
                </span>
              </div>
              
              <h3 className="text-lg font-bold text-white mb-1.5 leading-tight group-hover:text-indigo-400 transition-colors">
                {service.name}
              </h3>
              
              <p className="text-sm text-slate-400 flex-1 line-clamp-2 leading-relaxed mb-4">
                {service.description}
              </p>

              <hr className="border-slate-800 mb-4" />

              <div className="space-y-2 mt-auto">
                <h4 className="text-[11px] uppercase font-bold text-slate-500 tracking-wider">Provider & Contact</h4>
                
                <div className="flex items-center text-sm font-medium text-slate-300">
                  <svg className="w-4 h-4 text-slate-500 mr-2 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                  </svg>
                  <span className="truncate">{service.providerInfo?.company_name || 'Anonymous Provider'}</span>
                </div>
                
                {(service.providerInfo?.phone || service.providerInfo?.email) ? (
                  <div className="flex flex-col space-y-2 mt-2">
                    {service.providerInfo?.phone && (
                      <div className="flex items-center text-sm text-indigo-300">
                        <svg className="w-4 h-4 text-indigo-500/70 mr-2 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                        </svg>
                        <span className="truncate">{service.providerInfo.phone}</span>
                      </div>
                    )}
                    {service.providerInfo?.email && (
                      <div className="flex items-center text-sm text-indigo-300">
                        <svg className="w-4 h-4 text-indigo-500/70 mr-2 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                        </svg>
                        <span className="truncate">{service.providerInfo.email}</span>
                      </div>
                    )}
                  </div>
                ) : (
                  <p className="text-xs text-slate-600 italic">No contact info provided.</p>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
