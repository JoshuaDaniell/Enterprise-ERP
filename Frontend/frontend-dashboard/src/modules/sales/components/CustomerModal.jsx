import { useState, useEffect } from 'react';
import { getAllCustomers, createCustomer } from '../services/salesService';

const CustomerModal = ({ isOpen, onClose }) => {
    const [customers, setCustomers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isCreating, setIsCreating] = useState(false);
    const [error, setError] = useState(null);

    // New Customer Form State
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [companyName, setCompanyName] = useState('');
    const [email, setEmail] = useState('');
    const [phone, setPhone] = useState('');
    const [street, setStreet] = useState('');
    const [city, setCity] = useState('');
    const [state, setState] = useState('');
    const [postalCode, setPostalCode] = useState('');
    const [country] = useState('India');

    const loadCustomers = async () => {
        setLoading(true);
        try {
            const data = await getAllCustomers();
            setCustomers(Array.isArray(data) ? data : []);
            setError(null);
        } catch (err) {
            setError(err.response?.data?.message || err.message || 'Failed to fetch customers');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (isOpen) {
            // eslint-disable-next-line react-hooks/set-state-in-effect
            loadCustomers();
        }
    }, [isOpen]);

    const handleCreateCustomer = async (e) => {
        e.preventDefault();
        try {
            const newCust = await createCustomer({
                firstName,
                lastName,
                companyName,
                email,
                phone,
                addresses: [
                    {
                        addressType: 'BOTH',
                        street,
                        city,
                        state,
                        postalCode,
                        country,
                        isDefault: true
                    }
                ]
            });
            setCustomers(prev => [...prev, newCust]);
            setIsCreating(false);
            // Reset form
            setFirstName('');
            setLastName('');
            setCompanyName('');
            setEmail('');
            setPhone('');
            setStreet('');
            setCity('');
            setState('');
            setPostalCode('');
        } catch (err) {
            alert(err.response?.data?.message || err.message || 'Error creating customer');
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="bg-white rounded-xl shadow-2xl border border-gray-200 max-w-2xl w-full p-6 animate-in fade-in max-h-[90vh] overflow-y-auto">
                <div className="flex justify-between items-center border-b pb-3 mb-4">
                    <div>
                        <h3 className="text-lg font-bold text-gray-900">Customer Profiles & Directory</h3>
                        <p className="text-xs text-gray-500">Manage client accounts and delivery addresses</p>
                    </div>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl font-bold p-1">&times;</button>
                </div>

                {error && (
                    <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg">
                        ⚠️ {error}
                    </div>
                )}

                {!isCreating ? (
                    <div className="space-y-4">
                        <div className="flex justify-between items-center">
                            <span className="text-xs font-semibold text-gray-600 uppercase">
                                Registered Customers ({customers.length})
                            </span>
                            <button
                                onClick={() => setIsCreating(true)}
                                className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs rounded shadow transition"
                            >
                                + Add New Customer
                            </button>
                        </div>

                        {loading ? (
                            <div className="py-8 text-center text-xs text-gray-500">Loading customers...</div>
                        ) : customers.length === 0 ? (
                            <div className="py-8 text-center text-xs text-gray-500 border border-dashed rounded-lg">No customers registered yet.</div>
                        ) : (
                            <div className="divide-y border rounded-lg overflow-hidden">
                                {customers.map(c => (
                                    <div key={c.id} className="p-3.5 hover:bg-gray-50 flex items-center justify-between text-xs">
                                        <div>
                                            <div className="font-bold text-gray-900">{c.fullName}</div>
                                            <div className="text-gray-500 text-[11px]">{c.companyName ? `${c.companyName} • ` : ''}{c.email} • {c.phone || 'No phone'}</div>
                                            {c.addresses && c.addresses.length > 0 && (
                                                <div className="text-slate-400 text-[10px] mt-0.5">
                                                    📍 {c.addresses[0].street}, {c.addresses[0].city}, {c.addresses[0].state} {c.addresses[0].postalCode}
                                                </div>
                                            )}
                                        </div>
                                        <span className="px-2 py-0.5 bg-slate-100 font-mono text-[10px] text-slate-600 rounded">
                                            ID: #{c.id}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                ) : (
                    <form onSubmit={handleCreateCustomer} className="space-y-3">
                        <h4 className="text-sm font-bold text-gray-800">New Customer Details</h4>
                        <div className="grid grid-cols-2 gap-3">
                            <div>
                                <label className="block text-[11px] font-semibold text-gray-600 mb-1">First Name</label>
                                <input type="text" value={firstName} onChange={e => setFirstName(e.target.value)} required className="w-full p-2 border rounded text-xs focus:ring-1 focus:ring-blue-500" />
                            </div>
                            <div>
                                <label className="block text-[11px] font-semibold text-gray-600 mb-1">Last Name</label>
                                <input type="text" value={lastName} onChange={e => setLastName(e.target.value)} required className="w-full p-2 border rounded text-xs focus:ring-1 focus:ring-blue-500" />
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-3">
                            <div>
                                <label className="block text-[11px] font-semibold text-gray-600 mb-1">Company Name</label>
                                <input type="text" value={companyName} onChange={e => setCompanyName(e.target.value)} placeholder="e.g. Apex Global" className="w-full p-2 border rounded text-xs focus:ring-1 focus:ring-blue-500" />
                            </div>
                            <div>
                                <label className="block text-[11px] font-semibold text-gray-600 mb-1">Email</label>
                                <input type="email" value={email} onChange={e => setEmail(e.target.value)} required placeholder="client@domain.com" className="w-full p-2 border rounded text-xs focus:ring-1 focus:ring-blue-500" />
                            </div>
                        </div>

                        <div>
                            <label className="block text-[11px] font-semibold text-gray-600 mb-1">Phone</label>
                            <input type="text" value={phone} onChange={e => setPhone(e.target.value)} placeholder="+91 98765 43210" className="w-full p-2 border rounded text-xs focus:ring-1 focus:ring-blue-500" />
                        </div>

                        <div className="border-t pt-2 mt-2">
                            <label className="block text-[11px] font-bold text-gray-700 uppercase mb-2">Primary Address</label>
                            <div className="space-y-2">
                                <input type="text" value={street} onChange={e => setStreet(e.target.value)} placeholder="Street Address" required className="w-full p-2 border rounded text-xs focus:ring-1 focus:ring-blue-500" />
                                <div className="grid grid-cols-3 gap-2">
                                    <input type="text" value={city} onChange={e => setCity(e.target.value)} placeholder="City" required className="p-2 border rounded text-xs focus:ring-1 focus:ring-blue-500" />
                                    <input type="text" value={state} onChange={e => setState(e.target.value)} placeholder="State" required className="p-2 border rounded text-xs focus:ring-1 focus:ring-blue-500" />
                                    <input type="text" value={postalCode} onChange={e => setPostalCode(e.target.value)} placeholder="PIN / Postal Code" required className="p-2 border rounded text-xs focus:ring-1 focus:ring-blue-500" />
                                </div>
                            </div>
                        </div>

                        <div className="flex justify-end gap-2 pt-3 border-t">
                            <button type="button" onClick={() => setIsCreating(false)} className="px-3 py-1.5 border rounded text-xs font-semibold text-gray-600 hover:bg-gray-50">Cancel</button>
                            <button type="submit" className="px-4 py-1.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs rounded shadow">Save Customer</button>
                        </div>
                    </form>
                )}
            </div>
        </div>
    );
};

export default CustomerModal;
