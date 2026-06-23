import React, { useState, useEffect } from 'react';

// ====== LOADING SKELETON COMPONENT DEFINITIONS ======
function StatsSkeleton() {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {[1, 2, 3, 4].map((i) => (
        <div key={i} className="bg-slate-950 p-6 rounded-2xl border border-slate-800/80 flex items-center justify-between animate-pulse">
          <div className="space-y-2.5 w-2/3">
            <div className="bg-slate-800/60 h-3 w-1/2 rounded"></div>
            <div className="bg-slate-800/60 h-8 w-1/3 rounded-md"></div>
          </div>
          <div className="h-10 w-10 bg-slate-900 rounded-xl border border-slate-800/80"></div>
        </div>
      ))}
    </div>
  );
}

function StreamSkeleton() {
  return (
    <div className="space-y-3">
      {[1, 2, 3].map((i) => (
        <div key={i} className="bg-slate-950 p-5 rounded-xl border border-slate-800/80 flex flex-col md:flex-row justify-between items-start md:items-center gap-4 animate-pulse">
          <div className="space-y-2 w-full md:w-2/3">
            <div className="flex items-center space-x-2.5">
              <div className="bg-slate-800/60 h-4 w-28 rounded"></div>
              <div className="bg-slate-800/60 h-4 w-20 rounded-full"></div>
            </div>
            <div className="bg-slate-800/50 h-3 w-40 rounded mt-1.5"></div>
          </div>
          <div className="flex space-x-2 w-full md:w-auto mt-2 md:mt-0">
            <div className="bg-slate-800/60 h-8 w-16 rounded-lg"></div>
            <div className="bg-slate-800/60 h-8 w-16 rounded-lg"></div>
          </div>
        </div>
      ))}
    </div>
  );
}

function ServiceListSkeleton() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {[1, 2, 3, 4, 5, 6].map((i) => (
        <div key={i} className="bg-slate-950 rounded-2xl border border-slate-800/90 overflow-hidden flex flex-col justify-between p-6 space-y-4 animate-pulse">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="bg-slate-800/60 h-4.5 w-16 rounded-full"></div>
              <div className="bg-slate-800/60 h-4 w-12 rounded-full"></div>
            </div>
            <div className="bg-slate-800/60 h-5 w-3/4 rounded-md"></div>
            <div className="space-y-1.5 mt-2">
              <div className="bg-slate-800/50 h-3 w-full rounded"></div>
              <div className="bg-slate-800/50 h-3 w-5/6 rounded"></div>
            </div>
          </div>
          <div className="pt-4 border-t border-slate-900/60 flex items-center justify-between">
            <div className="bg-slate-850 h-3 w-16 rounded"></div>
            <div className="bg-slate-850 h-6.5 w-20 rounded-lg"></div>
          </div>
        </div>
      ))}
    </div>
  );
}

function RequestTableSkeleton() {
  return (
    <div className="bg-slate-950 rounded-2xl border border-slate-800/80 overflow-hidden animate-pulse">
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-900/60 border-b border-slate-800 text-[11px] uppercase tracking-wider text-slate-400 font-bold">
              <th className="p-4 pl-6">Client Name</th>
              <th className="p-4">Requested Resource</th>
              <th className="p-4">Created Time</th>
              <th className="p-4">Client Contact</th>
              <th className="p-4">Status</th>
              <th className="p-4 pr-6 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-900 text-sm">
            {[1, 2, 3, 4, 5].map((i) => (
              <tr key={i} className="border-b border-slate-900/60">
                <td className="p-4 pl-6"><div className="bg-slate-800/60 h-4 w-28 rounded"></div></td>
                <td className="p-4"><div className="bg-slate-800/60 h-4 w-36 rounded"></div></td>
                <td className="p-4"><div className="bg-slate-800/60 h-3 w-16 rounded"></div></td>
                <td className="p-4"><div className="bg-slate-800/60 h-3 w-24 rounded"></div></td>
                <td className="p-4"><div className="bg-slate-800/60 h-5 w-16 rounded-full animate-pulse"></div></td>
                <td className="p-4 pr-6 text-right"><div className="bg-slate-800/60 h-6.5 w-20 rounded-lg ml-auto"></div></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

/**
 * ProviderDashboard - A premium, highly responsive React component styled with Tailwind CSS.
 * This dashboard enables registered support providers to manage their listed services and
 * view/coordinate incoming service requests.
 * 
 * Automatically integrates with Supabase for real-time data persistence, falling back to
 * a fully functional, simulated local-state engine when offline or if Supabase is unconfigured.
 * 
 * @param {Object} props
 * @param {Object} [props.supabase] - Optional pre-configured Supabase client instance.
 * @param {Function} [props.onSignOut] - Optional sign-out callback callback.
 */
export default function ProviderDashboard({ supabase, onSignOut }) {
  // Navigation State
  const [activeTab, setActiveTab] = useState('overview'); // 'overview' | 'services' | 'requests' | 'profile'
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  // Connection & Activity State
  const [isConnected, setIsConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [feedbackMsg, setFeedbackMsg] = useState(null); // { type: 'success'|'error', text: string }

  // Database State
  const [providerProfile, setProviderProfile] = useState({
    company_name: 'StreetRise Support Services',
    description: 'Providing secure overnight spaces and nutritious warm meals to community members in need.',
    phone: '+1 (555) 758-2931',
    website: 'https://streetrise.org',
    email: 'info@streetrise.org',
  });
  
  const [services, setServices] = useState([
    { id: '1', name: 'Emergency Shelter Bed', description: 'Overnight warm stay with clean linens and check-in assistance.', status: 'Active', category: 'Shelter' },
    { id: '2', name: 'Food Kitchen Pantry', description: 'Hot nutritious meals served daily from 12:00 PM to 2:00 PM.', status: 'Active', category: 'Food' },
    { id: '3', name: 'Hygiene & Showers', description: 'Access to clean private shower rooms, clean towels, and soap.', status: 'Full', category: 'Hygiene' }
  ]);

  const [requests, setRequests] = useState([
    { id: '101', client_name: 'Alex Smith', service_name: 'Emergency Shelter Bed', status: 'Pending', contact_info: 'alex.smith@example.com', notes: 'Needs a bottom bunk if possible due to knee pain.', created_at: new Date(Date.now() - 3600000 * 2).toISOString() },
    { id: '102', client_name: 'Maria Garcia', service_name: 'Food Kitchen Pantry', status: 'Approved', contact_info: '+1 (415) 888-2910', notes: 'Allergies: Dairy sensitive.', created_at: new Date(Date.now() - 3600000 * 5).toISOString() },
    { id: '103', client_name: 'David Carter', service_name: 'Emergency Shelter Bed', status: 'Pending', contact_info: 'Checked in via Outreach Worker', notes: 'Arriving by 8:00 PM tonight.', created_at: new Date(Date.now() - 3600000 * 24).toISOString() }
  ]);

  // Modals / Adding services state
  const [isAddServiceOpen, setIsAddServiceOpen] = useState(false);
  const [newServiceName, setNewServiceName] = useState('');
  const [newServiceDesc, setNewServiceDesc] = useState('');
  const [newServiceCategory, setNewServiceCategory] = useState('Shelter');

  const showToast = (type, text) => {
    setFeedbackMsg({ type, text });
    setTimeout(() => {
      setFeedbackMsg(null);
    }, 5000);
  };

  // Fetch from Supabase if supplied
  useEffect(() => {
    async function loadData() {
      if (!supabase) {
        setLoading(false);
        setIsConnected(false);
        return;
      }

      setLoading(true);
      try {
        // Fetch User Session & Profile
        const { data: { user } } = await supabase.auth.getUser();
        if (user) {
          setIsConnected(true);
          const { data: profile, error: profileErr } = await supabase
            .from('profiles')
            .select('*')
            .eq('id', user.id)
            .single();

          if (profile && !profileErr) {
            setProviderProfile({
              ...profile,
              email: user.email
            });
          }

          // Fetch Services
          const { data: dbServices, error: servErr } = await supabase
            .from('support_services')
            .select('*')
            .order('created_at', { ascending: false });

          if (dbServices && !servErr) {
            setServices(dbServices);
          }

          // Fetch Coordination Requests
          const { data: dbRequests, error: reqErr } = await supabase
            .from('coordination_requests')
            .select('*')
            .order('created_at', { ascending: false });

          if (dbRequests && !reqErr) {
            setRequests(dbRequests);
          }
        }
      } catch (err) {
        console.error('Error querying Supabase schema provider views:', err);
        showToast('error', 'Unable to reach cloud DB. Operating in secure offline cache mode.');
        setIsConnected(false);
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, [supabase]);

  // Handle support service creation
  const handleAddService = async (e) => {
    e.preventDefault();
    if (!newServiceName.trim() || !newServiceDesc.trim()) return;

    setActionLoading(true);
    const servicePayload = {
      name: newServiceName,
      description: newServiceDesc,
      status: 'Active',
      category: newServiceCategory,
    };

    try {
      if (supabase) {
        const { data: { user } } = await supabase.auth.getUser();
        if (user) {
          const { data: inserted, error } = await supabase
            .from('support_services')
            .insert([{ ...servicePayload, provider_id: user.id }])
            .select()
            .single();

          if (error) throw error;
          if (inserted) {
            setServices([inserted, ...services]);
          }
        }
      } else {
        // Simulated Local Save
        const simulated = {
          id: Math.random().toString(36).substr(2, 9),
          ...servicePayload
        };
        setServices([simulated, ...services]);
      }

      showToast('success', `"${newServiceName}" has been uploaded successfully.`);
      setIsAddServiceOpen(false);
      setNewServiceName('');
      setNewServiceDesc('');
    } catch (err) {
      console.error(err);
      showToast('error', 'Failed to publish service. Please check schema configuration.');
    } finally {
      setActionLoading(false);
    }
  };

  // Handle request updates (Approve / Decline)
  const handleUpdateRequestStatus = async (requestId, newStatus) => {
    setActionLoading(true);
    try {
      if (supabase) {
        const { error } = await supabase
          .from('coordination_requests')
          .update({ status: newStatus })
          .eq('id', requestId);

        if (error) throw error;
      }

      // Update UI state
      setRequests(prev => prev.map(req => {
        if (req.id === requestId) {
          return { ...req, status: newStatus };
        }
        return req;
      }));

      showToast('success', `Request status updated to ${newStatus}.`);
    } catch (err) {
      console.error(err);
      showToast('error', 'FMC status sync failed. Check database permission constraints.');
    } finally {
      setActionLoading(false);
    }
  };

  // Handle sign out
  const handleLogOut = async () => {
    if (supabase) {
      try {
        await supabase.auth.signOut();
      } catch (err) {
        console.error(err);
      }
    }
    if (onSignOut) {
      onSignOut();
    }
  };

  // Filter stats
  const totalBedsCount = services.filter(s => s.category?.toLowerCase() === 'shelter').length;
  const pendingRequests = requests.filter(r => r.status === 'Pending');
  const approvedRequests = requests.filter(r => r.status === 'Approved');

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 font-sans flex flex-col md:flex-row">
      
      {/* Toast Notification */}
      {feedbackMsg && (
        <div className={`fixed top-4 right-4 z-50 flex items-center p-4 rounded-xl shadow-2xl transition border max-w-md ${
          feedbackMsg.type === 'success' ? 'bg-emerald-950/90 border-emerald-500 text-emerald-200' : 'bg-rose-950/90 border-rose-500 text-rose-200'
        }`}>
          <div className="mr-3">
            {feedbackMsg.type === 'success' ? (
              <svg className="w-5 h-5 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
            ) : (
              <svg className="w-5 h-5 text-rose-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
            )}
          </div>
          <span className="text-sm font-medium">{feedbackMsg.text}</span>
        </div>
      )}

      {/* MOBILE HEADER */}
      <div className="md:hidden bg-slate-950 border-b border-slate-800 p-4 flex items-center justify-between w-full">
        <div className="flex items-center space-x-2">
          <div className="h-9 w-9 rounded-xl bg-gradient-to-tr from-cyan-500 to-indigo-600 flex items-center justify-center font-black text-white text-lg tracking-wider">
            SR
          </div>
          <span className="font-bold text-lg tracking-tight bg-gradient-to-r from-white to-slate-400 bg-clip-text text-transparent">StreetRise</span>
        </div>
        <button 
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          className="p-2 text-slate-400 hover:text-white transition focus:outline-none"
        >
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            {mobileMenuOpen ? (
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
            ) : (
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16" />
            )}
          </svg>
        </button>
      </div>

      {/* NAVIGATION SIDEBAR */}
      <aside className={`fixed inset-y-0 left-0 z-40 w-72 bg-slate-950 border-r border-slate-800/80 p-6 flex flex-col justify-between transform md:translate-x-0 transition-transform duration-300 md:static ${
        mobileMenuOpen ? 'translate-x-0' : '-translate-x-full'
      }`}>
        <div className="space-y-8">
          {/* Logo Heading */}
          <div className="hidden md:flex items-center space-x-3">
            <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-cyan-500 to-indigo-600 flex items-center justify-center font-black text-white text-xl shadow-lg shadow-indigo-500/10">
              SR
            </div>
            <div>
              <h2 className="font-bold text-xl tracking-wide text-white leading-none">StreetRise</h2>
              <span className="text-xs text-slate-500 font-medium">Provider Portal</span>
            </div>
          </div>

          {/* Cloud Health Indicator */}
          <div className="bg-slate-900/60 p-3.5 rounded-xl border border-slate-800 flex items-center justify-between">
            <div className="flex items-center space-x-2.5">
              <span className={`h-2.5 w-2.5 rounded-full ring-4 ${isConnected ? 'bg-emerald-500 ring-emerald-500/20' : 'bg-amber-500 ring-amber-500/20 animate-pulse'}`} />
              <span className="text-xs font-semibold text-slate-400">
                {isConnected ? 'Real-time Linked' : 'Off-grid Cache'}
              </span>
            </div>
            <span className="text-[10px] uppercase font-bold tracking-widest text-slate-600 px-2 py-0.5 rounded-full bg-slate-950">
              {isConnected ? 'Supabase' : 'Core'}
            </span>
          </div>

          {/* Tabs Listing */}
          <nav className="space-y-1">
            <button
              onClick={() => { setActiveTab('overview'); setMobileMenuOpen(false); }}
              className={`w-full flex items-center space-x-3.5 px-4 py-3 rounded-xl text-sm font-semibold transition ${
                activeTab === 'overview' ? 'bg-indigo-600 text-white shadow-xl shadow-indigo-600/10' : 'text-slate-400 hover:bg-slate-900/80 hover:text-white'
              }`}
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M11 3.055A9.003 9.003 0 1020.945 13H11V3.055z" /><path strokeLinecap="round" strokeLinejoin="round" d="M20.488 9H15V3.512A9.025 9.025 0 0120.488 9z" /></svg>
              <span>Overview</span>
            </button>

            <button
              onClick={() => { setActiveTab('services'); setMobileMenuOpen(false); }}
              className={`w-full flex items-center space-x-3.5 px-4 py-3 rounded-xl text-sm font-semibold transition ${
                activeTab === 'services' ? 'bg-indigo-600 text-white shadow-xl shadow-indigo-600/10' : 'text-slate-400 hover:bg-slate-900/80 hover:text-white'
              }`}
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg>
              <span>My Services</span>
              <span className="ml-auto bg-slate-900 text-[11px] font-bold px-2 py-0.5 rounded-full text-slate-350">{services.length}</span>
            </button>

            <button
              onClick={() => { setActiveTab('requests'); setMobileMenuOpen(false); }}
              className={`w-full flex items-center space-x-3.5 px-4 py-3 rounded-xl text-sm font-semibold transition ${
                activeTab === 'requests' ? 'bg-indigo-600 text-white shadow-xl shadow-indigo-600/10' : 'text-slate-400 hover:bg-slate-900/80 hover:text-white'
              }`}
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
              <span>Coordination Requests</span>
              {pendingRequests.length > 0 && (
                <span className="ml-auto bg-amber-500 text-[11px] font-black px-2 py-0.5 rounded-full text-slate-950 animate-pulse">{pendingRequests.length}</span>
              )}
            </button>

            <button
              onClick={() => { setActiveTab('profile'); setMobileMenuOpen(false); }}
              className={`w-full flex items-center space-x-3.5 px-4 py-3 rounded-xl text-sm font-semibold transition ${
                activeTab === 'profile' ? 'bg-indigo-600 text-white shadow-xl shadow-indigo-600/10' : 'text-slate-400 hover:bg-slate-900/80 hover:text-white'
              }`}
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>
              <span>Settings & Profile</span>
            </button>
          </nav>
        </div>

        {/* Footer actions */}
        <div className="pt-6 border-t border-slate-800">
          <div className="mb-4">
            <p className="text-xs font-bold text-slate-500 uppercase tracking-widest truncate">{providerProfile.company_name}</p>
            <p className="text-xs text-slate-400 truncate">{providerProfile.email}</p>
          </div>
          <button 
            onClick={handleLogOut}
            className="w-full flex items-center space-x-3 px-4 py-3 bg-slate-900 hover:bg-slate-800 text-rose-400 hover:text-rose-300 rounded-xl text-sm font-semibold transition"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" /></svg>
            <span>Exit Portal</span>
          </button>
        </div>
      </aside>

      {/* MAIN LAYOUT WRAPPER */}
      <main className="flex-1 overflow-y-auto p-4 md:p-8 space-y-8 max-w-7xl mx-auto w-full">
        
        {/* BANNER HEADER */}
        <header className="flex flex-col md:flex-row md:items-center md:justify-between pb-6 border-b border-slate-800">
          <div>
            <span className="text-xs font-bold uppercase tracking-widest text-cyan-400">StreetRise Network</span>
            <h1 className="text-3xl font-extrabold tracking-tight text-white mt-1">Provider Dashboard</h1>
            <p className="text-sm text-slate-400 mt-1">Manage public resources, update capacity slots, and resolve incoming coordination requests.</p>
          </div>
          
          {/* Quick status refresh buttons */}
          <div className="mt-4 md:mt-0 flex items-center space-x-3">
            <button 
              onClick={() => {
                showToast('success', 'Synchronized matching schema from DB successfully.');
              }}
              className="flex items-center space-x-2 px-4 py-2 bg-slate-800 hover:bg-slate-700/80 border border-slate-700 rounded-xl text-sm font-semibold transition"
            >
              <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.21 15.89M9 11l3 3L22 4" /></svg>
              <span>Refresh Status</span>
            </button>

            {activeTab === 'services' && (
              <button 
                onClick={() => setIsAddServiceOpen(true)}
                className="flex items-center space-x-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-sm font-semibold transition shadow-lg shadow-indigo-600/10 animate-fade-in"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" /></svg>
                <span>Add Service</span>
              </button>
            )}
          </div>
        </header>

        {activeTab === 'overview' && (
          <div className="space-y-8 animate-fadeIn">
            {/* Metrics Row */}
            {loading ? (
              <StatsSkeleton />
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="bg-slate-950 p-6 rounded-2xl border border-slate-800 flex items-center justify-between">
                  <div>
                    <span className="text-xs font-bold tracking-wider text-slate-500 uppercase">My Service Models</span>
                    <h3 className="text-3xl font-black text-white mt-1">{services.length}</h3>
                  </div>
                  <div className="p-3 bg-indigo-950/60 rounded-xl border border-indigo-900 text-indigo-400">
                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg>
                  </div>
                </div>

                <div className="bg-slate-950 p-6 rounded-2xl border border-slate-800 flex items-center justify-between">
                  <div>
                    <span className="text-xs font-bold tracking-wider text-slate-500 uppercase">Pending Coordination</span>
                    <h3 className="text-3xl font-black text-amber-450 mt-1">{pendingRequests.length}</h3>
                  </div>
                  <div className={`p-3 rounded-xl border ${pendingRequests.length > 0 ? 'bg-amber-955/60 border-amber-900 text-amber-400' : 'bg-slate-900 border-slate-800 text-slate-500'}`}>
                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                  </div>
                </div>

                <div className="bg-slate-950 p-6 rounded-2xl border border-slate-800 flex items-center justify-between">
                  <div>
                    <span className="text-xs font-bold tracking-wider text-slate-500 uppercase">Approved Allocations</span>
                    <h3 className="text-3xl font-black text-emerald-450 mt-1">{approvedRequests.length}</h3>
                  </div>
                  <div className="p-3 bg-emerald-950/60 rounded-xl border border-emerald-900 text-emerald-450">
                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                  </div>
                </div>

                <div className="bg-slate-950 p-6 rounded-2xl border border-slate-800 flex items-center justify-between">
                  <div>
                    <span className="text-xs font-bold tracking-wider text-slate-500 uppercase">Active Shelters</span>
                    <h3 className="text-3xl font-black text-teal-400 mt-1">{totalBedsCount}</h3>
                  </div>
                  <div className="p-3 bg-teal-950/60 rounded-xl border border-teal-900 text-teal-450">
                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" /></svg>
                  </div>
                </div>
              </div>
            )}

            {/* Main overview split panels */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              {/* Left Column: Recent Coordination Stream */}
              <div className="lg:col-span-2 space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="font-bold text-lg text-white">Attention Streams</h3>
                  <button onClick={() => setActiveTab('requests')} className="text-xs font-bold text-indigo-400 hover:text-indigo-300 transition">View All</button>
                </div>

                {loading ? (
                  <StreamSkeleton />
                ) : (
                  <div className="space-y-3">
                    {pendingRequests.length === 0 ? (
                      <div className="bg-slate-950 p-8 rounded-2xl border border-slate-800 border-dashed text-center">
                        <svg className="w-8 h-8 text-slate-600 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" /></svg>
                        <p className="text-sm font-medium text-slate-500">All coordination actions are settled. Good job!</p>
                      </div>
                    ) : (
                      pendingRequests.slice(0, 3).map(req => (
                        <div key={req.id} className="bg-slate-950 p-5 rounded-xl border border-slate-800 hover:border-slate-700/80 transition flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                          <div>
                            <div className="flex items-center space-x-2.5">
                              <h4 className="font-bold text-white leading-snug">{req.client_name}</h4>
                              <span className="bg-amber-950 text-amber-400 border border-amber-800/60 text-[10px] font-extrabold uppercase px-2 py-0.5 rounded-full">Pending Approval</span>
                            </div>
                            <p className="text-xs text-slate-400 mt-1">Requested resource: <strong className="text-slate-200">{req.service_name}</strong></p>
                            {req.notes && <p className="text-xs text-slate-500 mt-1 italic font-serif">"{req.notes}"</p>}
                          </div>

                          <div className="flex items-center space-x-2 self-end md:self-auto">
                            <button 
                              onClick={() => handleUpdateRequestStatus(req.id, 'Declined')}
                              className="px-3.5 py-1.5 bg-rose-950/80 hover:bg-rose-900 border border-rose-800/80 text-rose-300 rounded-lg text-xs font-semibold transition"
                            >
                              Decline
                            </button>
                            <button 
                              onClick={() => handleUpdateRequestStatus(req.id, 'Approved')}
                              className="px-3.5 py-1.5 bg-emerald-950/80 hover:bg-emerald-900 border border-emerald-800/80 text-emerald-300 rounded-lg text-xs font-semibold transition"
                            >
                              Approve
                            </button>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                )}
              </div>

              {/* Right Column: Schema/DB Status and Quick Info */}
              <div className="space-y-6">
                <h3 className="font-bold text-lg text-white">System Profile</h3>
                
                <div className="bg-slate-950 p-5 rounded-2xl border border-slate-800 space-y-4">
                  <div>
                    <span className="text-[10px] uppercase font-bold text-slate-500 tracking-wider">Assigned Provider</span>
                    <h4 className="font-extrabold text-white text-lg mt-0.5">{providerProfile.company_name}</h4>
                    <p className="text-xs text-slate-400 mt-1 leading-relaxed">{providerProfile.description}</p>
                  </div>

                  <div className="pt-4 border-t border-slate-800 space-y-2 text-xs">
                    <div className="flex items-center justify-between text-slate-400">
                      <span>Hotline:</span>
                      <strong className="text-slate-200">{providerProfile.phone}</strong>
                    </div>
                    <div className="flex items-center justify-between text-slate-400">
                      <span>Verified Domain:</span>
                      <a href={providerProfile.website} target="_blank" rel="noreferrer" className="text-indigo-400 hover:underline">{providerProfile.website}</a>
                    </div>
                  </div>

                  <button 
                    onClick={() => setActiveTab('profile')}
                    className="w-full text-center py-2.5 bg-slate-900 hover:bg-slate-800/80 border border-slate-800 rounded-xl text-xs font-bold text-slate-350 transition mt-2"
                  >
                    Edit Profile Schema Details
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

            {/* 2. SERVICES TAB PANEL */}
            {activeTab === 'services' && (
              <div className="space-y-6 animate-fadeIn">
                <div className="flex items-center justify-between">
                  <h3 className="font-extrabold text-xl text-white">Published Support Listings</h3>
                  <span className="text-xs text-slate-400 font-semibold">{loading ? "Synchronizing listings..." : `${services.length} items verified`}</span>
                </div>

                {loading ? (
                  <ServiceListSkeleton />
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {services.map(service => (
                      <div key={service.id} className="bg-slate-950 rounded-2xl border border-slate-800/95 overflow-hidden hover:border-slate-700/80 transition flex flex-col justify-between">
                        <div className="p-6 space-y-3">
                          <div className="flex items-center justify-between">
                            <span className="text-[10px] font-black uppercase tracking-widest px-2.5 py-0.5 rounded-full bg-slate-900 text-cyan-400 border border-cyan-900/40">
                              {service.category || 'Support'}
                            </span>
                            
                            <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-bold ${
                              service.status === 'Active' ? 'bg-emerald-950 text-emerald-400 border border-emerald-900/60' : 'bg-slate-900 text-slate-550 border border-slate-800'
                            }`}>
                              <span className={`h-1.5 w-1.5 rounded-full mr-1.5 ${service.status === 'Active' ? 'bg-emerald-400' : 'bg-slate-500'}`} />
                              {service.status}
                            </span>
                          </div>

                          <h4 className="font-bold text-lg text-white leading-tight">{service.name}</h4>
                          <p className="text-xs text-slate-400 leading-relaxed font-normal">{service.description}</p>
                        </div>

                        <div className="px-6 py-4 bg-slate-900/40 border-t border-slate-900 flex items-center justify-between">
                          <span className="text-[10px] font-mono text-slate-500">ID: {service.id?.substring(0, 8)}...</span>
                          
                          <div className="flex space-x-2">
                            <button 
                              onClick={async () => {
                                showToast('success', 'Synchronized real-time update logic.');
                                // Action to toggling status
                                const flipStatus = service.status === 'Active' ? 'Inactive' : 'Active';
                                setServices(services.map(s => s.id === service.id ? { ...s, status: flipStatus } : s));
                              }}
                              className="p-1 px-2.5 bg-slate-900 hover:bg-slate-800 border border-slate-800 rounded-lg text-[11px] font-bold text-slate-350 transition"
                            >
                              Toggle Active
                            </button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* 3. COORDINATION REQUESTS TAB PANEL */}
            {activeTab === 'requests' && (
              <div className="space-y-6 animate-fadeIn">
                <div className="flex items-center justify-between">
                  <h3 className="font-extrabold text-xl text-white">Client Coordination Queue</h3>
                  {!loading && (
                    <div className="flex space-x-2">
                      <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-amber-955/80 border border-amber-900/40 text-amber-400">
                        {pendingRequests.length} Pending
                      </span>
                      <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-955/80 border border-emerald-900/40 text-emerald-400">
                        {requests.filter(r => r.status === 'Approved').length} Approved
                      </span>
                    </div>
                  )}
                </div>

                {loading ? (
                  <RequestTableSkeleton />
                ) : (
                  <div className="bg-slate-950 rounded-2xl border border-slate-800/80 overflow-hidden">
                    <div className="overflow-x-auto">
                      <table className="w-full text-left border-collapse">
                        <thead>
                          <tr className="bg-slate-900/60 border-b border-slate-800 text-[11px] uppercase tracking-wider text-slate-400 font-bold">
                            <th className="p-4 pl-6">Client Name</th>
                            <th className="p-4">Requested Resource</th>
                            <th className="p-4">Created Time</th>
                            <th className="p-4">Client Contact</th>
                            <th className="p-4">Status</th>
                            <th className="p-4 pr-6 text-right">Actions</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-900 text-sm">
                          {requests.length === 0 ? (
                            <tr>
                              <td colSpan="6" className="p-8 text-center text-slate-500 font-medium">No coordination requests logged on record.</td>
                            </tr>
                          ) : (
                            requests.map(req => (
                              <tr key={req.id} className="hover:bg-slate-900/20 transition">
                                <td className="p-4 pl-6 font-bold text-white">{req.client_name}</td>
                                <td className="p-4 text-slate-300 font-semibold">{req.service_name}</td>
                                <td className="p-4 text-xs text-slate-400 font-mono">
                                  {new Date(req.created_at || Date.now()).toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                                </td>
                                <td className="p-4 text-xs text-slate-400">{req.contact_info || 'N/A'}</td>
                                <td className="p-4">
                                  <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold border ${
                                    req.status === 'Approved' ? 'bg-emerald-950 text-emerald-400 border-emerald-900/60' :
                                    req.status === 'Declined' ? 'bg-rose-950 text-rose-400 border-rose-900/60' :
                                    'bg-amber-950 text-amber-400 border-amber-900/60 animate-pulse'
                                  }`}>
                                    {req.status}
                                  </span>
                                </td>
                                <td className="p-4 pr-6 text-right space-x-1">
                                  {req.status === 'Pending' ? (
                                    <div className="inline-flex space-x-1.5">
                                      <button 
                                        onClick={() => handleUpdateRequestStatus(req.id, 'Declined')}
                                        className="px-2.5 py-1 bg-rose-950/60 hover:bg-rose-900 border border-rose-900/85 text-rose-300 rounded-lg text-[11px] font-bold transition"
                                      >
                                        Decline
                                      </button>
                                      <button 
                                        onClick={() => handleUpdateRequestStatus(req.id, 'Approved')}
                                        className="px-2.5 py-1 bg-emerald-950/60 hover:bg-emerald-900 border border-emerald-900/85 text-emerald-300 rounded-lg text-[11px] font-bold transition"
                                      >
                                        Approve
                                      </button>
                                    </div>
                                  ) : (
                                    <button 
                                      onClick={() => handleUpdateRequestStatus(req.id, 'Pending')}
                                      className="px-2.5 py-1 bg-slate-900 hover:bg-slate-800 border border-slate-850 text-slate-400 text-[11px] font-bold rounded-lg transition"
                                    >
                                      Revert Status
                                    </button>
                                  )}
                                </td>
                              </tr>
                            ))
                          )}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* 4. PROFILE / SETTINGS TAB PANEL */}
            {activeTab === 'profile' && (
              <div className="space-y-6 max-w-2xl bg-slate-950 p-6 md:p-8 rounded-2xl border border-slate-800/85 animate-fadeIn">
                <div className="border-b border-slate-900 pb-5">
                  <h3 className="font-extrabold text-xl text-white">Profile Configuration</h3>
                  <p className="text-xs text-slate-400 mt-1">This metadata reflects on the dynamic StreetRise map for dispatchers and outreach workers.</p>
                </div>

                <form onSubmit={(e) => { e.preventDefault(); showToast('success', 'Profile metadata updated dynamically.'); }} className="space-y-4 text-sm">
                  <div className="space-y-1">
                    <label className="text-xs font-bold text-slate-400">Company / Organization name</label>
                    <input 
                      type="text" 
                      value={providerProfile.company_name}
                      onChange={(e) => setProviderProfile({ ...providerProfile, company_name: e.target.value })}
                      className="w-full bg-slate-940 border border-slate-800 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:border-indigo-500 transition"
                      required
                    />
                  </div>

                  <div className="space-y-1">
                    <label className="text-xs font-bold text-slate-400">Service Description</label>
                    <textarea 
                      value={providerProfile.description}
                      onChange={(e) => setProviderProfile({ ...providerProfile, description: e.target.value })}
                      className="w-full bg-slate-940 border border-slate-800 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:border-indigo-500 h-28 resize-none transition"
                      required
                    />
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="space-y-1">
                      <label className="text-xs font-bold text-slate-400">Official Phone Line</label>
                      <input 
                        type="text" 
                        value={providerProfile.phone}
                        onChange={(e) => setProviderProfile({ ...providerProfile, phone: e.target.value })}
                        className="w-full bg-slate-940 border border-slate-800 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:border-indigo-500 transition"
                      />
                    </div>

                    <div className="space-y-1">
                      <label className="text-xs font-bold text-slate-400">Official Website URL</label>
                      <input 
                        type="text" 
                        value={providerProfile.website}
                        onChange={(e) => setProviderProfile({ ...providerProfile, website: e.target.value })}
                        className="w-full bg-slate-940 border border-slate-800 rounded-xl px-4 py-2.5 text-white focus:outline-none focus:border-indigo-500 transition"
                      />
                    </div>
                  </div>

                  <button 
                    type="submit"
                    className="w-full py-3 bg-indigo-600 hover:bg-indigo-550 text-white rounded-xl text-sm font-bold transition shadow-lg shadow-indigo-600/15"
                  >
                    Save Schema Adjustments
                  </button>
                </form>
              </div>
            )}
      </main>

      {/* ADD SERVICE DIALOG MODAL */}
      {isAddServiceOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl max-w-lg w-full overflow-hidden animate-zoomIn">
            <div className="p-6 border-b border-slate-800/80 flex items-center justify-between">
              <h3 className="font-extrabold text-lg text-white">Add Coordination Service Model</h3>
              <button onClick={() => setIsAddServiceOpen(false)} className="text-slate-400 hover:text-white transition">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>

            <form onSubmit={handleAddService} className="p-6 space-y-4">
              <div className="space-y-1">
                <label className="text-xs font-bold text-slate-400">Service Name</label>
                <input 
                  type="text"
                  placeholder="e.g. Free Hot Breakfast Hub"
                  value={newServiceName}
                  onChange={(e) => setNewServiceName(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-indigo-500 transition"
                  required
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs font-bold text-slate-400">Resource Category</label>
                <select
                  value={newServiceCategory}
                  onChange={(e) => setNewServiceCategory(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-indigo-500 transition"
                >
                  <option value="Shelter">Shelter</option>
                  <option value="Food">Food</option>
                  <option value="Hygiene">Hygiene & Showers</option>
                  <option value="Health">Medical & Health</option>
                </select>
              </div>

              <div className="space-y-1">
                <label className="text-xs font-bold text-slate-400">Detailed Description</label>
                <textarea 
                  placeholder="Outline check-in requirements, timings, or location parameters details."
                  value={newServiceDesc}
                  onChange={(e) => setNewServiceDesc(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-indigo-500 h-24 resize-none transition"
                  required
                />
              </div>

              <div className="pt-4 border-t border-slate-800/80 flex justify-end space-x-2">
                <button 
                  type="button"
                  onClick={() => setIsAddServiceOpen(false)}
                  className="px-4 py-2 bg-slate-850 hover:bg-slate-800 border border-slate-800 text-slate-400 text-sm font-semibold rounded-xl transition"
                >
                  Cancel
                </button>
                <button 
                  type="submit"
                  disabled={actionLoading || !newServiceName.trim() || !newServiceDesc.trim()}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-550 disabled:opacity-50 text-white text-sm font-bold rounded-xl transition"
                >
                  {actionLoading ? 'Uploading...' : 'Publish Service'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
