import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { createOrder } from '../../api/orders';

const CustomerCart = ({ onCartUpdate }) => {
  const [cart, setCart] = useState(() => {
    const saved = localStorage.getItem('cart');
    return saved ? JSON.parse(saved) : [];
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(null);
  const [animatingItems, setAnimatingItems] = useState(new Set());
  const navigate = useNavigate();

  useEffect(() => {
    localStorage.setItem('cart', JSON.stringify(cart));
    updateCartCount();
  }, [cart]);

  const updateCartCount = () => {
    const count = cart.reduce((sum, item) => sum + item.quantity, 0);
    onCartUpdate(count);
  };

  const updateQuantity = (productId, newQuantity) => {
    if (newQuantity <= 0) {
      setShowDeleteConfirm(productId);
      return;
    }

    // アニメーション効果
    setAnimatingItems(prev => new Set([...prev, productId]));
    setTimeout(() => {
      setAnimatingItems(prev => {
        const newSet = new Set(prev);
        newSet.delete(productId);
        return newSet;
      });
    }, 300);

    setCart(cart.map(item =>
      item.productId === productId
        ? { ...item, quantity: newQuantity }
        : item
    ));
  };

  const removeFromCart = (productId) => {
    // アニメーション効果
    setAnimatingItems(prev => new Set([...prev, productId]));
    setTimeout(() => {
      setCart(cart.filter(item => item.productId !== productId));
      setShowDeleteConfirm(null);
    }, 300);
  };

  const getTotal = () => {
    return cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);
  };

  const getProductImage = (productId) => {
    const colors = [
      'bg-gradient-to-br from-blue-400 to-blue-600',
      'bg-gradient-to-br from-green-400 to-green-600',
      'bg-gradient-to-br from-purple-400 to-purple-600',
      'bg-gradient-to-br from-pink-400 to-pink-600',
      'bg-gradient-to-br from-indigo-400 to-indigo-600',
      'bg-gradient-to-br from-red-400 to-red-600'
    ];
    return colors[productId % colors.length];
  };

  const handleCheckout = async () => {
    if (cart.length === 0) {
      setError('カートが空です');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const orderData = {
        items: cart.map(item => ({
          productId: item.productId,
          quantity: item.quantity
        }))
      };

      const response = await createOrder(orderData);
      setSuccess(`注文が完了しました！注文ID: ${response.orderId}, 合計金額: ¥${response.totalPrice.toLocaleString()}`);

      // カートをクリア
      setCart([]);
      localStorage.removeItem('cart');

      // 3秒後に商品一覧に戻る
      setTimeout(() => {
        navigate('/customer');
      }, 3000);
    } catch (err) {
      setError(err.response?.data?.message || '注文の処理に失敗しました');
    } finally {
      setLoading(false);
    }
  };

  if (cart.length === 0) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {success && (
          <div className="mb-8 bg-green-50 border border-green-200 text-green-700 px-6 py-4 rounded-xl flex items-center justify-center gap-2 animate-pulse shadow-lg max-w-2xl mx-auto">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
            <span className="font-bold text-lg">{success}</span>
          </div>
        )}
        <div className="text-center">
          <div className="mb-8">
            <h1 className="text-4xl font-bold text-gray-900 mb-2 bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
              ショッピングカート
            </h1>
            <p className="text-gray-600">カートに商品がありません</p>
          </div>

          <div className="bg-white rounded-2xl shadow-xl p-16 max-w-lg mx-auto transform hover:scale-105 transition-all duration-300">
            <div className="text-8xl mb-8 animate-bounce">🛒</div>
            <h3 className="text-2xl font-bold text-gray-900 mb-4">カートが空です</h3>
            <p className="text-gray-600 mb-8 text-lg">お気に入りの商品をカートに追加してください</p>
            <button
              className="bg-gradient-to-r from-blue-600 to-blue-700 text-white px-10 py-4 rounded-xl font-semibold hover:from-blue-700 hover:to-blue-800 transform hover:scale-105 transition-all duration-300 shadow-lg hover:shadow-xl flex items-center gap-2 mx-auto"
              onClick={() => navigate('/customer')}
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16l-4-4m0 0l4-4m-4 4h18" />
              </svg>
              商品一覧を見る
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <h1 className="text-4xl font-bold text-gray-900 mb-2 bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
          ショッピングカート
        </h1>
        <p className="text-gray-600">{cart.length}点の商品</p>
      </div>

      {error && (
        <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center gap-2">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          {error}
        </div>
      )}

      {success && (
        <div className="mb-6 bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg flex items-center gap-2 animate-pulse">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
          {success}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          <div className="bg-white rounded-2xl shadow-xl overflow-hidden">
            {cart.map((item, index) => (
              <div
                key={item.productId}
                className={`p-6 transition-all duration-300 ${
                  animatingItems.has(item.productId) ? 'bg-blue-50 scale-105' : ''
                } ${index !== cart.length - 1 ? 'border-b border-gray-100' : ''}`}
              >
                <div className="flex items-center gap-6">
                  {/* Product Image */}
                  <div className={`w-20 h-20 rounded-xl ${getProductImage(item.productId)} flex items-center justify-center text-white font-bold text-lg shadow-lg`}>
                    #{item.productId}
                  </div>

                  <div className="flex-1">
                    <h3 className="text-xl font-bold text-gray-900 mb-2">{item.name}</h3>
                    <p className="text-gray-600 text-lg">
                      ¥{item.price.toLocaleString()} × {item.quantity} = <span className="font-semibold text-blue-600">¥{(item.price * item.quantity).toLocaleString()}</span>
                    </p>
                  </div>

                  <div className="flex items-center gap-4">
                    {/* Quantity Controls */}
                    <div className="flex items-center border-2 border-gray-300 rounded-xl overflow-hidden bg-gray-50">
                      <button
                        className="px-4 py-3 text-gray-600 hover:text-gray-800 hover:bg-gray-100 transition-all duration-200 font-bold text-lg"
                        onClick={() => updateQuantity(item.productId, item.quantity - 1)}
                      >
                        −
                      </button>
                      <span className="px-6 py-3 text-center min-w-[60px] bg-white border-x border-gray-300 font-bold text-lg">
                        {item.quantity}
                      </span>
                      <button
                        className="px-4 py-3 text-gray-600 hover:text-gray-800 hover:bg-gray-100 transition-all duration-200 font-bold text-lg"
                        onClick={() => updateQuantity(item.productId, item.quantity + 1)}
                      >
                        +
                      </button>
                    </div>

                    {/* Delete Button */}
                    <button
                      className="text-red-500 hover:text-red-700 p-3 hover:bg-red-50 rounded-xl transition-all duration-200 group"
                      onClick={() => setShowDeleteConfirm(item.productId)}
                      title="削除"
                    >
                      <svg className="w-6 h-6 group-hover:scale-110 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="lg:col-span-1">
          <div className="bg-white rounded-2xl shadow-xl p-8 sticky top-4">
            <h3 className="text-2xl font-bold text-gray-900 mb-6 flex items-center gap-2">
              <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              注文合計
            </h3>

            <div className="space-y-4 mb-8">
              <div className="flex justify-between items-center text-lg">
                <span className="text-gray-600">小計</span>
                <span className="text-gray-900 font-medium">¥{getTotal().toLocaleString()}</span>
              </div>
              <div className="flex justify-between items-center text-lg">
                <span className="text-gray-600">送料</span>
                <span className="text-green-600 font-medium flex items-center gap-1">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  無料
                </span>
              </div>
              <hr className="border-gray-200 border-dashed" />
              <div className="flex justify-between items-center text-2xl font-bold">
                <span className="text-gray-900">合計</span>
                <span className="text-green-600">¥{getTotal().toLocaleString()}</span>
              </div>
            </div>

            <button
              className="w-full bg-gradient-to-r from-green-600 to-green-700 text-white py-5 px-8 rounded-xl font-bold text-lg hover:from-green-700 hover:to-green-800 transform hover:scale-[1.02] transition-all duration-300 shadow-lg hover:shadow-xl disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none flex items-center justify-center gap-3"
              onClick={handleCheckout}
              disabled={loading}
            >
              {loading ? (
                <>
                  <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-white"></div>
                  注文処理中...
                </>
              ) : (
                <>
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" />
                  </svg>
                  注文を確定する
                </>
              )}
            </button>

            <div className="mt-6 p-4 bg-blue-50 rounded-xl border border-blue-200">
              <div className="flex items-center gap-2 text-blue-800 text-sm">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span className="font-medium">注文確定後は変更できません</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50 flex items-center justify-center">
          <div className="bg-white rounded-2xl shadow-2xl p-8 max-w-md mx-4 transform animate-in zoom-in-95 duration-300">
            <div className="text-center">
              <div className="text-red-500 text-6xl mb-4">⚠️</div>
              <h3 className="text-xl font-bold text-gray-900 mb-2">商品を削除しますか？</h3>
              <p className="text-gray-600 mb-6">この操作は取り消すことができません。</p>

              <div className="flex gap-4">
                <button
                  onClick={() => setShowDeleteConfirm(null)}
                  className="flex-1 bg-gray-100 hover:bg-gray-200 text-gray-800 py-3 px-6 rounded-xl font-medium transition-colors"
                >
                  キャンセル
                </button>
                <button
                  onClick={() => removeFromCart(showDeleteConfirm)}
                  className="flex-1 bg-red-600 hover:bg-red-700 text-white py-3 px-6 rounded-xl font-medium transition-colors"
                >
                  削除する
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default CustomerCart;

