"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { Loader2, ArrowLeft, CreditCard, ShieldCheck } from "lucide-react";

export default function YajmanCheckout() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const bookingId = searchParams.get("bookingId");
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);
  const [booking, setBooking] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!bookingId) {
      router.push("/yajman/bookings");
      return;
    }

    const token = localStorage.getItem("jwt_auth_token");
    if (!token) {
      router.push("/yajman/login");
      return;
    }

    fetchBookingDetails(token);
    loadRazorpayScript();
  }, [bookingId, router]);

  const loadRazorpayScript = () => {
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.async = true;
    document.body.appendChild(script);
  };

  const fetchBookingDetails = async (token) => {
    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoritrial.somee.com/api/v1";
      const res = await fetch(`${baseUrl}/booking/${bookingId}`, {
        headers: { "Authorization": `Bearer ${token}` }
      });
      if (!res.ok) throw new Error("Failed to fetch booking details.");
      const data = await res.json();
      setBooking(data.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handlePayment = async () => {
    if (!booking) return;
    setPaying(true);
    setError("");

    try {
      const token = localStorage.getItem("jwt_auth_token");
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoritrial.somee.com/api/v1";

      // 1. Create Razorpay Order in Backend
      const orderRes = await fetch(`${baseUrl}/payment/create-order`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify({ bookingId: booking.id })
      });

      if (!orderRes.ok) throw new Error("Failed to generate order ID from server.");
      const orderData = await orderRes.json();

      // 2. Configure Razorpay Options
      const options = {
        key: orderData.keyId,
        amount: orderData.amount, // amount in paisa
        currency: "INR",
        name: "दिंडोरी प्रणीत यज्ञिकी",
        description: booking.poojaName || "Seva Payment",
        order_id: orderData.orderId,
        handler: async function (response) {
          // 3. Verify Payment on Backend
          try {
            const verifyRes = await fetch(`${baseUrl}/payment/verify`, {
              method: "POST",
              headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
              },
              body: JSON.stringify({
                bookingId: booking.id,
                razorpayPaymentId: response.razorpay_payment_id,
                razorpaySignature: response.razorpay_signature
              })
            });

            if (verifyRes.ok) {
              router.push("/yajman/bookings?payment=success");
            } else {
              throw new Error("Payment signature verification failed.");
            }
          } catch (verErr) {
            setError(verErr.message);
            setPaying(false);
          }
        },
        prefill: {
          name: booking.contactName,
          contact: booking.contactPhone
        },
        theme: {
          color: "#ea580c" // Orange theme
        }
      };

      const rzp = new window.Razorpay(options);
      rzp.open();
    } catch (err) {
      setError(err.message);
      setPaying(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 text-slate-500">
        <Loader2 className="animate-spin text-orange-600 mr-2" /> Loading Checkout...
      </div>
    );
  }

  if (error || !booking) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 p-6">
        <div className="bg-white border p-8 rounded-3xl text-center max-w-md w-full shadow">
          <p className="text-red-500 font-bold mb-4">{error || "Error loading checkout page"}</p>
          <Link href="/yajman/bookings" className="text-orange-600 font-bold hover:underline">
            Back to Bookings
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-700 p-6 font-sans">
      <div className="max-w-md mx-auto">
        <Link href="/yajman/bookings" className="flex items-center gap-1.5 text-slate-500 hover:text-slate-800 text-xs font-bold uppercase tracking-wider mb-6">
          <ArrowLeft size={16} /> मागे जा (Back)
        </Link>

        <div className="bg-white border border-slate-200 rounded-[35px] p-8 shadow-sm">
          <div className="bg-orange-500/10 w-12 h-12 rounded-full flex items-center justify-center mb-6 text-orange-600">
            <CreditCard size={24} />
          </div>

          <h2 className="text-2xl font-black text-slate-800 uppercase tracking-wide mb-1">पेमेंट पूर्ण करा</h2>
          <p className="text-slate-400 text-xs mb-6">Seva Payment Authorization Gateway</p>

          <div className="bg-slate-50 p-6 rounded-2xl border mb-6 space-y-3">
            <div className="flex justify-between items-center text-xs">
              <span className="text-slate-400 font-bold uppercase">पूजा विधी:</span>
              <span className="font-bold text-slate-800">{booking.poojaName}</span>
            </div>
            <div className="flex justify-between items-center text-xs">
              <span className="text-slate-400 font-bold uppercase">गुरुजी:</span>
              <span className="font-bold text-slate-800">{booking.gurujiName || "Assigned Guruji"}</span>
            </div>
            <div className="flex justify-between items-center text-xs pt-3 border-t border-slate-200">
              <span className="text-slate-500 font-black uppercase text-sm">एकूण दक्षिणा:</span>
              <span className="font-black text-orange-600 text-base">₹ {booking.amount?.toFixed(2)}</span>
            </div>
          </div>

          <button 
            onClick={handlePayment} 
            disabled={paying}
            className="w-full bg-orange-600 hover:bg-orange-700 text-white py-4 rounded-2xl font-bold uppercase tracking-wider transition-all shadow-lg shadow-orange-600/10 flex items-center justify-center gap-2"
          >
            {paying ? <Loader2 className="animate-spin text-white" size={20} /> : <>दक्षिणा द्या (Pay Now) <ShieldCheck size={18} /></>}
          </button>
        </div>
      </div>
    </div>
  );
}
