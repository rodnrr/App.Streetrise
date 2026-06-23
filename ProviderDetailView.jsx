import React, { useState } from 'react';

export default function ProviderDetailView({ provider }) {
  const [reviews, setReviews] = useState(provider?.reviews || []);
  const [isWritingReview, setIsWritingReview] = useState(false);
  const [rating, setRating] = useState(0);
  const [feedback, setFeedback] = useState('');

  if (!provider) return null;

  const handleSubmitReview = (e) => {
    e.preventDefault();
    if (rating === 0 || feedback.trim() === '') return;

    const newReview = {
      id: Date.now().toString(),
      authorName: 'You',
      rating,
      feedback,
      date: 'Just now'
    };

    setReviews([newReview, ...reviews]);
    setIsWritingReview(false);
    setRating(0);
    setFeedback('');
  };

  return (
    <div className="bg-slate-950 p-8 rounded-2xl border border-slate-800 space-y-6 max-w-2xl mx-auto w-full">
      {/* Header section */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-black text-white">{provider.company_name || 'Unknown Provider'}</h2>
          {provider.is_verified && (
            <div className="inline-flex items-center mt-2 px-3 py-1 rounded-full bg-emerald-950/60 border border-emerald-900/60 text-emerald-400 text-xs font-bold w-fit">
              <svg className="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
              Verified Provider
            </div>
          )}
        </div>
      </div>

      <p className="text-sm text-slate-400 leading-relaxed font-medium">
        {provider.description || 'No description available for this provider.'}
      </p>

      <hr className="border-slate-800/80" />

      {/* Contact Info */}
      <div>
        <h3 className="text-sm uppercase font-bold text-slate-500 tracking-wider mb-4">Contact Information</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          
          <div className="flex items-start space-x-3">
            <div className="p-2 bg-indigo-950/50 rounded-lg text-indigo-400 border border-indigo-900/50">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" /></svg>
            </div>
            <div>
              <p className="text-xs text-slate-500 font-semibold mb-0.5">Primary Phone</p>
              <p className="text-sm text-slate-200 font-medium">{provider.phone || 'N/A'}</p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <div className="p-2 bg-indigo-950/50 rounded-lg text-indigo-400 border border-indigo-900/50">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" /></svg>
            </div>
            <div>
              <p className="text-xs text-slate-500 font-semibold mb-0.5">Email Address</p>
              <p className="text-sm text-slate-200 font-medium">{provider.email || 'N/A'}</p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <div className="p-2 bg-indigo-950/50 rounded-lg text-indigo-400 border border-indigo-900/50">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9-3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9" /></svg>
            </div>
            <div>
              <p className="text-xs text-slate-500 font-semibold mb-0.5">Website</p>
              {provider.website ? (
                <a href={provider.website} target="_blank" rel="noopener noreferrer" className="text-sm text-indigo-400 hover:text-indigo-300 transition-colors font-medium">
                  {provider.website}
                </a>
              ) : (
                <p className="text-sm text-slate-200 font-medium">N/A</p>
              )}
            </div>
          </div>
          
          <div className="flex items-start space-x-3">
            <div className="p-2 bg-indigo-950/50 rounded-lg text-indigo-400 border border-indigo-900/50">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
            </div>
            <div>
              <p className="text-xs text-slate-500 font-semibold mb-0.5">Location Address</p>
              <p className="text-sm text-slate-200 font-medium">{provider.address || 'N/A'}</p>
            </div>
          </div>

        </div>
      </div>

      <hr className="border-slate-800/80" />

      {/* Operating Hours */}
      <div>
        <h3 className="text-sm uppercase font-bold text-slate-500 tracking-wider mb-4">Operating Hours</h3>
        <div className="bg-slate-900/50 rounded-xl p-4 border border-slate-800">
          <p className="text-sm text-slate-300 font-medium leading-relaxed whitespace-pre-line">
            {provider.operating_hours || 'Standard Business Hours'}
          </p>
        </div>
      </div>

      <hr className="border-slate-800/80" />

      {/* Reviews Section */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm uppercase font-bold text-slate-500 tracking-wider">Reviews</h3>
          <button 
            type="button"
            onClick={() => setIsWritingReview(!isWritingReview)} 
            className="text-xs font-bold text-indigo-400 hover:text-indigo-300 transition-colors"
          >
            {isWritingReview ? 'Cancel' : 'Leave a Review'}
          </button>
        </div>

        {isWritingReview && (
          <form onSubmit={handleSubmitReview} className="bg-slate-900/50 rounded-xl p-5 border border-slate-800 mb-6 space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-400 mb-2">Rating</label>
              <div className="flex space-x-1">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setRating(star)}
                    className="focus:outline-none"
                  >
                    <svg 
                      className={`w-6 h-6 ${star <= rating ? 'text-amber-400' : 'text-slate-600'}`} 
                      fill="currentColor" 
                      viewBox="0 0 20 20"
                    >
                      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                    </svg>
                  </button>
                ))}
              </div>
            </div>
            
            <div>
              <label className="block text-xs font-bold text-slate-400 mb-2">Your Experience</label>
              <textarea
                value={feedback}
                onChange={(e) => setFeedback(e.target.value)}
                placeholder="Share your experience..."
                className="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-sm text-slate-200 placeholder-slate-600 focus:outline-none focus:border-indigo-500/50"
                rows="3"
              />
            </div>
            
            <div className="flex justify-end">
              <button 
                type="submit" 
                disabled={rating === 0 || feedback.trim() === ''}
                className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-800 disabled:text-slate-500 text-white text-xs font-bold rounded-lg transition-colors"
              >
                Submit Review
              </button>
            </div>
          </form>
        )}

        {reviews.length === 0 ? (
          <p className="text-sm text-slate-400 italic">No reviews yet. Be the first to leave one!</p>
        ) : (
          <div className="space-y-4">
            {reviews.map((review) => (
              <div key={review.id} className="bg-slate-900/30 rounded-lg p-4 border border-slate-800/80">
                <div className="flex justify-between items-start mb-2">
                  <div>
                    <h4 className="text-sm font-bold text-slate-200">{review.authorName}</h4>
                    <p className="text-xs text-slate-500">{review.date}</p>
                  </div>
                  <div className="flex space-x-0.5">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <svg 
                        key={star}
                        className={`w-4 h-4 ${star <= review.rating ? 'text-amber-400' : 'text-slate-700'}`} 
                        fill="currentColor" 
                        viewBox="0 0 20 20"
                      >
                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                      </svg>
                    ))}
                  </div>
                </div>
                <p className="text-sm text-slate-300">
                  {review.feedback}
                </p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
