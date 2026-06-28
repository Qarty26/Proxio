import { useState, useEffect } from "react";
import { apiUrl } from "./apiBase";

const inputStyle = (hasError) => ({
  width: "100%", boxSizing: "border-box",
  background: "#0d0d0d", border: `1.5px solid ${hasError ? "#ff3b3b" : "#2a2a2a"}`,
  borderRadius: 6, padding: "11px 14px", color: "#f0f0f0",
  fontFamily: "'DM Mono', monospace", fontSize: 14, outline: "none",
  transition: "border-color .2s",
});

function loadUser() {
  try {
    const s = localStorage.getItem("proxio_user");
    return s ? JSON.parse(s) : null;
  } catch { return null; }
}
const selectStyle = {
  width: "100%", boxSizing: "border-box",
  background: "#0d0d0d", border: "1.5px solid #2a2a2a",
  borderRadius: 6, padding: "11px 14px", color: "#f0f0f0",
  fontFamily: "'DM Mono', monospace", fontSize: 14, outline: "none",
  cursor: "pointer",
};

const buttonStyle = (variant = "primary") => ({
  padding: "8px 16px",
  background: variant === "primary" ? "#f0f0f0" : "transparent",
  color: variant === "primary" ? "#111" : "#f0f0f0",
  border: variant === "primary" ? "none" : "1px solid #2a2a2a",
  borderRadius: 6,
  cursor: "pointer",
  fontFamily: "'DM Mono', monospace",
  fontSize: 12,
  transition: "all .2s",
});

function Field({ label, error, children }) {
  return (
    <div style={{ marginBottom: 20 }}>
      <label style={{
        display: "block", fontSize: 10, letterSpacing: "0.14em",
        textTransform: "uppercase", color: "#555", marginBottom: 7,
        fontFamily: "'DM Mono', monospace"
      }}>{label}</label>
      {children}
      {error && (
        <div style={{ color: "#ff3b3b", fontSize: 12, marginTop: 5, fontFamily: "'DM Mono', monospace" }}>
          ⚠ {error}
        </div>
      )}
    </div>
  );
}

function Modal({ isOpen, onClose, title, children }) {
  if (!isOpen) return null;
  
  return (
    <div style={{
      position: "fixed", top: 0, left: 0, right: 0, bottom: 0,
      background: "rgba(0,0,0,0.8)", backdropFilter: "blur(4px)",
      display: "flex", alignItems: "center", justifyContent: "center",
      zIndex: 1000, padding: 20,
    }} onClick={onClose}>
      <div style={{
        background: "#111", border: "1px solid #1e1e1e",
        borderRadius: 12, maxWidth: 500, width: "100%",
        maxHeight: "90vh", overflow: "auto",
      }} onClick={e => e.stopPropagation()}>
        <div style={{
          padding: "24px 28px", borderBottom: "1px solid #1e1e1e",
          display: "flex", justifyContent: "space-between", alignItems: "center",
        }}>
          <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: 24, color: "#f0f0f0" }}>{title}</h2>
          <button onClick={onClose} style={{ background: "none", border: "none", color: "#888", fontSize: 24, cursor: "pointer" }}>×</button>
        </div>
        <div style={{ padding: "28px" }}>{children}</div>
      </div>
    </div>
  );
}

export default function Products({ user, onLogout }) {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [currentVendor, setCurrentVendor] = useState(null);
  const [isVendor, setIsVendor] = useState(false);
  
  // Pagination state
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  
  // Sorting state
  const [sortBy, setSortBy] = useState("name");
  const [sortDir, setSortDir] = useState("asc");
  
  // Filter state
  const [categoryFilter, setCategoryFilter] = useState("");
  
  // Form states
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isStockModalOpen, setIsStockModalOpen] = useState(false);
  const [stockProduct, setStockProduct] = useState(null);
  const [isLocationsModalOpen, setIsLocationsModalOpen] = useState(false);
  const [isOrderModalOpen, setIsOrderModalOpen] = useState(false);
  const [orderProduct, setOrderProduct] = useState(null);
  const [isMyOrdersOpen, setIsMyOrdersOpen] = useState(false);
  const [isVendorOrdersOpen, setIsVendorOrdersOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [formData, setFormData] = useState({
    name: "", 
    description: "", 
    unit: "", 
    category: "VEGETABLES", 
    imageUrl: ""
  });
  const [formErrors, setFormErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [submitting, setSubmitting] = useState(false);

  // Categories
  const categories = ["VEGETABLES", "OTHERS", "HONEY", "FRUITS", "DAIRY", "MEAT"];

  // Helper function to get category color for badges
  const getCategoryColor = (category) => {
    const colors = {
      VEGETABLES: "#4caf50",
      OTHERS: "#9e9e9e",
      HONEY: "#ff9800",
      FRUITS: "#ff5722",
      DAIRY: "#2196f3",
      MEAT: "#f44336"
    };
    return colors[category] || "#888";
  };

  const canManageProduct = (product) => {
    const storedUser = loadUser();
    if (!storedUser) return false;
    if (storedUser.role === "ADMIN") return true;
    return storedUser.role === "VENDOR" && currentVendor?.id && product.vendor?.id === currentVendor.id;
  };

  // Fetch current vendor info
  const fetchCurrentVendor = async () => {
    try {
      const storedUser = loadUser();
      if (!storedUser || storedUser.role !== "VENDOR") {
        setIsVendor(false);
        return null;
      }

      const res = await fetch(apiUrl(`/api/vendors/by-user/${storedUser.id}`), {
        method: "GET",
        credentials: "include",
      });

      if (res.status === 404) {
        const vendor = await createVendorProfile(storedUser);
        setCurrentVendor(vendor);
        setIsVendor(true);
        return vendor;
      }

      if (!res.ok) throw new Error("Failed to fetch vendor");

      const vendor = await res.json();
      setCurrentVendor(vendor);
      setIsVendor(true);
      return vendor;
    } catch (err) {
      console.error("Failed to fetch vendor:", err);
      setIsVendor(false);
      return null;
    }
  };

  const ensureCurrentVendor = async () => {
    if (currentVendor) return currentVendor;

    const vendor = await fetchCurrentVendor();
    if (!vendor) {
      throw new Error("Vendor profile is required before creating products");
    }

    return vendor;
  };

  const createVendorProfile = async (storedUser) => {
    const token = getXsrfToken();
    const profileName = storedUser.username || storedUser.email || "Vendor";

    const res = await fetch(apiUrl("/api/vendors"), {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        ...(token && { "X-XSRF-TOKEN": token }),
      },
      body: JSON.stringify({
        user: { id: storedUser.id },
        farmName: `${profileName} Farm`,
        description: "Local vendor profile",
      }),
    });

    if (!res.ok) {
      const errorData = await parseErrorResponse(res);
      throw new Error(formatServerError(errorData, "Failed to create vendor profile"));
    }

    return res.json();
  };

  const fetchProducts = async () => {
    setLoading(true);
    setError("");
    
    try {
      let url = apiUrl(`/api/products/paged?page=${page}&size=${size}&sortBy=${sortBy}&sortDir=${sortDir}`);
      if (categoryFilter) {
        url += `&category=${categoryFilter}`;
      }

      const res = await fetch(url, {
        method: "GET",
        credentials: "include",
      });
      
      if (res.status === 401) {
        onLogout();
        return;
      }
      
      if (!res.ok) throw new Error("Failed to fetch products");
      
      const data = await res.json();
      setProducts(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCurrentVendor();
  }, []);

  useEffect(() => {
    fetchProducts();
  }, [page, size, sortBy, sortDir, categoryFilter, currentVendor]);

  const validateForm = (data) => {
    const errors = {};
    if (!data.name.trim()) errors.name = "Product name is required";
    if (data.name.length < 2) errors.name = "Name must be at least 2 characters";
    if (!data.description.trim()) errors.description = "Description is required";
    if (data.description.length < 10) errors.description = "Description must be at least 10 characters";
    if (!data.unit.trim()) errors.unit = "Unit is required";
    if (!data.category) errors.category = "Category is required";
    return errors;
  };

  const handleInputChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (touched[field]) {
      const errors = validateForm({ ...formData, [field]: value });
      setFormErrors(prev => ({ ...prev, [field]: errors[field] }));
    }
  };

  const handleBlur = (field) => {
    setTouched(prev => ({ ...prev, [field]: true }));
    const errors = validateForm(formData);
    setFormErrors(prev => ({ ...prev, [field]: errors[field] }));
  };

  const handleCreate = async () => {
    const errors = validateForm(formData);
    setFormErrors(errors);
    setTouched({ name: true, description: true, unit: true, category: true });
    
    if (Object.keys(errors).length > 0) return;
    
    setSubmitting(true);
    try {
      const token = getXsrfToken();
      // Create product with current vendor ID
      
      const vendor = await ensureCurrentVendor();

      const productToCreate = {
        name: formData.name,
        description: formData.description,
        unit: formData.unit,
        category: formData.category,
        imageUrl: formData.imageUrl,
        vendor: { id: vendor.id }
      };
      
      const res = await fetch(apiUrl("/api/products"), {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          ...(token && { "X-XSRF-TOKEN": token }),
        },
        body: JSON.stringify(productToCreate),
      });
      
      if (!res.ok) {
        const errorData = await parseErrorResponse(res);
        throw new Error(formatServerError(errorData, "Failed to create product"));
      }
      
      setIsCreateModalOpen(false);
      resetForm();
      fetchProducts();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdate = async () => {
    const errors = validateForm(formData);
    setFormErrors(errors);
    setTouched({ name: true, description: true, unit: true, category: true });
    
    if (Object.keys(errors).length > 0) return;
    
    setSubmitting(true);
    try {
      const token = getXsrfToken();
      const vendor = await ensureCurrentVendor();
      
      const productToUpdate = {
        name: formData.name,
        description: formData.description,
        unit: formData.unit,
        category: formData.category,
        imageUrl: formData.imageUrl,
        vendor: { id: vendor.id }
      };
      
      const res = await fetch(apiUrl(`/api/products/${selectedProduct.id}`), {
        method: "PUT",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          ...(token && { "X-XSRF-TOKEN": token }),
        },
        body: JSON.stringify(productToUpdate),
      });
      
      if (!res.ok) {
        const errorData = await parseErrorResponse(res);
        throw new Error(formatServerError(errorData, "Failed to update product"));
      }
      
      setIsEditModalOpen(false);
      resetForm();
      fetchProducts();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Are you sure you want to delete this product?")) return;
    
    try {
      const token = getXsrfToken();
      const res = await fetch(apiUrl(`/api/products/${id}`), {
        method: "DELETE",
        credentials: "include",
        headers: token ? { "X-XSRF-TOKEN": token } : {},
      });
      
      if (!res.ok) throw new Error("Failed to delete product");
      
      fetchProducts();
    } catch (err) {
      setError(err.message);
    }
  };

  const openEditModal = (product) => {
    setSelectedProduct(product);
    setFormData({
      name: product.name,
      description: product.description || "",
      unit: product.unit || "",
      category: product.category || "VEGETABLES",
      imageUrl: product.imageUrl || "",
    });
    setFormErrors({});
    setTouched({});
    setIsEditModalOpen(true);
  };

  const openStockModal = (product) => {
    setStockProduct(product);
    setIsStockModalOpen(true);
  };

  const openOrderModal = (product) => {
    setOrderProduct(product);
    setIsOrderModalOpen(true);
  };

  const resetForm = () => {
    setFormData({ 
      name: "", 
      description: "", 
      unit: "", 
      category: "VEGETABLES", 
      imageUrl: ""
    });
    setFormErrors({});
    setTouched({});
    setSelectedProduct(null);
  };

  const getXsrfToken = () => {
    const cookie = document.cookie.split("; ").find(c => c.startsWith("XSRF-TOKEN="));
    return cookie ? decodeURIComponent(cookie.split("=")[1]) : "";
  };

  const formatServerError = (errorData, fallback) => {
    if (!errorData) return fallback;
    if (typeof errorData === "string") return errorData;
    if (errorData.message) return errorData.message;
    const messages = Object.values(errorData).filter(Boolean);
    return messages.length ? messages.join("; ") : fallback;
  };

  const parseErrorResponse = async (res) => {
    const text = await res.text();
    if (!text) return null;

    try {
      return JSON.parse(text);
    } catch {
      return text;
    }
  };

  return (
    <div style={{
      minHeight: "100vh", background: "#0a0a0a",
      fontFamily: "'DM Mono', monospace", padding: 24,
    }}>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;700;900&family=DM+Mono:wght@400;500&display=swap');
        * { margin:0; padding:0; box-sizing:border-box; }
        body { background:#0a0a0a; }
        input:focus, select:focus, textarea:focus { border-color: #555 !important; outline: none; }
        @keyframes fadeUp { from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:none} }
      `}</style>

      <div style={{
        position: "fixed", inset: 0, pointerEvents: "none",
        backgroundImage: "linear-gradient(#1a1a1a 1px,transparent 1px),linear-gradient(90deg,#1a1a1a 1px,transparent 1px)",
        backgroundSize: "48px 48px", opacity: 0.35,
      }} />

      <div style={{ position: "relative", maxWidth: 1400, margin: "0 auto" }}>
        {/* Header */}
        <div style={{
          display: "flex", justifyContent: "space-between", alignItems: "center",
          marginBottom: 40, paddingBottom: 20, borderBottom: "1px solid #1e1e1e",
          flexWrap: "wrap", gap: 16,
        }}>
          <div>
            <div style={{ fontSize: 11, letterSpacing: "0.22em", color: "#444", textTransform: "uppercase", marginBottom: 8 }}>
              Proxio Marketplace
            </div>
            <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: 42, color: "#f0f0f0", fontWeight: 900 }}>
              Products
            </h1>
            {currentVendor && (
              <div style={{ fontSize: 12, color: "#4caf50", marginTop: 8 }}>
                Vendor: {currentVendor.farmName}
              </div>
            )}
          </div>
          <div style={{ display: "flex", gap: 12 }}>
            {loadUser()?.role === "CUSTOMER" && (
              <button onClick={() => setIsMyOrdersOpen(true)} style={buttonStyle("secondary")}>
                My Orders
              </button>
            )}
            {/* Only show these buttons for vendors */}
            {loadUser()?.role === "VENDOR" && (
              <>
                <button onClick={() => setIsVendorOrdersOpen(true)} style={buttonStyle("secondary")}>
                  Orders
                </button>
                <button onClick={() => setIsLocationsModalOpen(true)} style={buttonStyle("secondary")}>
                  Locations
                </button>
                <button onClick={() => setIsCreateModalOpen(true)} style={buttonStyle("primary")}>
                  + New Product
                </button>
              </>
            )}
            <button onClick={onLogout} style={buttonStyle("secondary")}>
              Sign Out
            </button>
          </div>
        </div>

        {/* Controls */}
        <div style={{
          background: "#111", border: "1px solid #1e1e1e", borderRadius: 12,
          padding: "20px 24px", marginBottom: 32,
          display: "flex", justifyContent: "space-between", alignItems: "center",
          flexWrap: "wrap", gap: 16,
        }}>
          <div style={{ display: "flex", gap: 16, alignItems: "flex-end", flexWrap: "wrap" }}>
            <div>
              <label style={{ fontSize: 10, color: "#555", display: "block", marginBottom: 4 }}>Items per page</label>
              <select value={size} onChange={(e) => setSize(Number(e.target.value))} style={selectStyle}>
                <option value={5}>5</option>
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
              </select>
            </div>
            <div>
              <label style={{ fontSize: 10, color: "#555", display: "block", marginBottom: 4 }}>Sort by</label>
              <select value={sortBy} onChange={(e) => setSortBy(e.target.value)} style={selectStyle}>
                <option value="name">Name</option>
                <option value="category">Category</option>
                <option value="id">ID</option>
              </select>
            </div>
            <div>
              <label style={{ fontSize: 10, color: "#555", display: "block", marginBottom: 4 }}>Direction</label>
              <select value={sortDir} onChange={(e) => setSortDir(e.target.value)} style={selectStyle}>
                <option value="asc">Ascending</option>
                <option value="desc">Descending</option>
              </select>
            </div>
            <div>
              <label style={{ fontSize: 10, color: "#555", display: "block", marginBottom: 4 }}>Category Filter</label>
              <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)} style={selectStyle}>
                <option value="">All Categories</option>
                {categories.map(cat => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </div>
          </div>
          <div>
            <span style={{ color: "#555", fontSize: 12 }}>
              Total: {totalElements} products
            </span>
          </div>
        </div>

        {/* Products Grid */}
        {error && (
          <div style={{
            background: "#ff3b3b11", border: "1px solid #ff3b3b33",
            borderRadius: 7, padding: "12px 16px", marginBottom: 24,
            color: "#ff3b3b", fontSize: 13,
          }}>⚠ {error}</div>
        )}

        {loading ?  (
          <div style={{ textAlign: "center", padding: 80, color: "#555" }}>Loading products...</div>
        ) : products.length === 0 ? (
          <div style={{ textAlign: "center", padding: 80, color: "#555" }}>
            {isVendor ? "No products found. Click 'New Product' to get started!" : "No products available."}
          </div>
        ) : (
          <div style={{
            display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(320px, 1fr))",
            gap: 24, marginBottom: 40,
          }}>
            {products.map((product) => (
              <div key={product.id} style={{
                background: "#111", border: "1px solid #1e1e1e", borderRadius: 12,
                overflow: "hidden", transition: "transform .2s, border-color .2s",
                animation: "fadeUp .45s ease both",
              }}>
                {product.imageUrl && (
                  <div style={{
                    height: 200, background: "#0d0d0d",
                    display: "flex", alignItems: "center", justifyContent: "center",
                    overflow: "hidden",
                  }}>
                    <img src={product.imageUrl} alt={product.name} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                  </div>
                )}
                <div style={{ padding: 20 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: 12 }}>
                    <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: 20, color: "#f0f0f0" }}>
                      {product.name}
                    </h3>
                    <span style={{
                      background: getCategoryColor(product.category),
                      padding: "4px 10px", borderRadius: 4,
                      fontSize: 10, color: "#fff", textTransform: "uppercase",
                      fontWeight: 500,
                    }}>{product.category}</span>
                  </div>
                  <p style={{ color: "#aaa", fontSize: 13, lineHeight: 1.5, marginBottom: 16 }}>
                    {product.description}
                  </p>
                  {product.unit && (
                    <div style={{ fontSize: 12, color: "#555", marginBottom: 16 }}>
                      Unit: {product.unit}
                    </div>
                  )}
                  {loadUser()?.role === "CUSTOMER" && (
                    <div style={{ borderTop: "1px solid #1e1e1e", paddingTop: 16 }}>
                      <button onClick={() => openOrderModal(product)} style={{ ...buttonStyle("primary"), width: "100%", padding: "9px" }}>
                        Order
                      </button>
                    </div>
                  )}
                  {canManageProduct(product) && (
                    <div style={{ display: "flex", gap: 8, borderTop: "1px solid #1e1e1e", paddingTop: 16 }}>
                      <button onClick={() => openStockModal(product)} style={{ ...buttonStyle("secondary"), flex: 1, padding: "8px" }}>
                        Stock
                      </button>
                      <button onClick={() => openEditModal(product)} style={{ ...buttonStyle("secondary"), flex: 1, padding: "8px" }}>
                        Edit
                      </button>
                      <button onClick={() => handleDelete(product.id)} style={{ ...buttonStyle("secondary"), flex: 1, padding: "8px", color: "#ff3b3b", borderColor: "#ff3b3b33" }}>
                        Delete
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Pagination */}
        {totalPages > 0 && (
          <div style={{
            display: "flex", justifyContent: "center", alignItems: "center",
            gap: 12, padding: "24px 0", flexWrap: "wrap",
          }}>
            <button
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
              style={{ ...buttonStyle("secondary"), opacity: page === 0 ? 0.3 : 1 }}
            >
              ← Previous
            </button>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap", justifyContent: "center" }}>
              {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                let pageNum;
                if (totalPages <= 5) pageNum = i;
                else if (page < 3) pageNum = i;
                else if (page > totalPages - 3) pageNum = totalPages - 5 + i;
                else pageNum = page - 2 + i;
                
                return (
                  <button
                    key={pageNum}
                    onClick={() => setPage(pageNum)}
                    style={{
                      ...buttonStyle("secondary"),
                      background: page === pageNum ? "#f0f0f0" : "transparent",
                      color: page === pageNum ? "#111" : "#f0f0f0",
                      minWidth: 36,
                    }}
                  >
                    {pageNum + 1}
                  </button>
                );
              })}
            </div>
            <button
              onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
              disabled={page === totalPages - 1}
              style={{ ...buttonStyle("secondary"), opacity: page === totalPages - 1 ? 0.3 : 1 }}
            >
              Next →
            </button>
          </div>
        )}
      </div>

      {/* Create Modal */}
      <Modal isOpen={isCreateModalOpen} onClose={() => setIsCreateModalOpen(false)} title="Create Product">
        <ProductForm
          formData={formData}
          formErrors={formErrors}
          categories={categories}
          onChange={handleInputChange}
          onBlur={handleBlur}
          onSubmit={handleCreate}
          submitting={submitting}
          submitText="Create Product"
        />
      </Modal>

      {/* Edit Modal */}
      <Modal isOpen={isEditModalOpen} onClose={() => setIsEditModalOpen(false)} title="Edit Product">
        <ProductForm
          formData={formData}
          formErrors={formErrors}
          categories={categories}
          onChange={handleInputChange}
          onBlur={handleBlur}
          onSubmit={handleUpdate}
          submitting={submitting}
          submitText="Update Product"
        />
      </Modal>

      {/* Place Order Modal */}
      <Modal
        isOpen={isOrderModalOpen}
        onClose={() => { setIsOrderModalOpen(false); setOrderProduct(null); }}
        title={`Order — ${orderProduct?.name || ""}`}
      >
        <PlaceOrderModal
          product={orderProduct}
          getXsrfToken={getXsrfToken}
          parseErrorResponse={parseErrorResponse}
          formatServerError={formatServerError}
          onSuccess={() => { setIsOrderModalOpen(false); setOrderProduct(null); }}
        />
      </Modal>

      {/* My Orders Modal */}
      <Modal isOpen={isMyOrdersOpen} onClose={() => setIsMyOrdersOpen(false)} title="My Orders">
        <OrderListModal
          fetchUrl="/api/orders/my"
          emptyMessage="You have no orders yet."
          getXsrfToken={getXsrfToken}
          parseErrorResponse={parseErrorResponse}
          formatServerError={formatServerError}
        />
      </Modal>

      {/* Vendor Incoming Orders Modal */}
      <Modal isOpen={isVendorOrdersOpen} onClose={() => setIsVendorOrdersOpen(false)} title="Incoming Orders">
        <OrderListModal
          fetchUrl="/api/orders/vendor"
          emptyMessage="No orders yet."
          canDeliver
          getXsrfToken={getXsrfToken}
          parseErrorResponse={parseErrorResponse}
          formatServerError={formatServerError}
        />
      </Modal>

      {/* Locations Modal */}
      <Modal
        isOpen={isLocationsModalOpen}
        onClose={() => setIsLocationsModalOpen(false)}
        title="My Locations"
      >
        <LocationsModalContent
          currentVendor={currentVendor}
          onLocationsChanged={fetchCurrentVendor}
          getXsrfToken={getXsrfToken}
          parseErrorResponse={parseErrorResponse}
          formatServerError={formatServerError}
        />
      </Modal>

      {/* Stock Modal */}
      <Modal
        isOpen={isStockModalOpen}
        onClose={() => { setIsStockModalOpen(false); setStockProduct(null); }}
        title={`Stock — ${stockProduct?.name || ""}`}
      >
        <StockModalContent
          product={stockProduct}
          currentVendor={currentVendor}
          getXsrfToken={getXsrfToken}
          parseErrorResponse={parseErrorResponse}
          formatServerError={formatServerError}
        />
      </Modal>
    </div>
  );
}

// ── Place Order Modal ──────────────────────────────────────────────────────

function PlaceOrderModal({ product, getXsrfToken, parseErrorResponse, formatServerError, onSuccess }) {
  const [stocks, setStocks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedStock, setSelectedStock] = useState(null);
  const [quantity, setQuantity] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [pickupSlots, setPickupSlots] = useState([]);
  const [selectedSlotId, setSelectedSlotId] = useState(null);

  const fmtDT = (dt) => dt ? new Date(dt).toLocaleString([], { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" }) : "";

  useEffect(() => {
    if (!product) return;
    setLoading(true);
    fetch(apiUrl(`/api/stocks?productId=${product.id}`), { credentials: "include" })
      .then(r => r.json())
      .then(data => {
        const available = data.filter(s => s.quantity > 0);
        setStocks(available);
        if (available.length === 1) setSelectedStock(available[0]);
      })
      .catch(() => setError("Failed to load available locations."))
      .finally(() => setLoading(false));
  }, [product?.id]);

  useEffect(() => {
    if (!selectedStock?.location?.id) { setPickupSlots([]); setSelectedSlotId(null); return; }
    const now = new Date();
    fetch(apiUrl(`/api/pickup-slots?locationId=${selectedStock.location.id}`), { credentials: "include" })
      .then(r => r.json())
      .then(data => {
        const future = data.filter(s => new Date(s.startTime) > now);
        future.sort((a, b) => new Date(a.startTime) - new Date(b.startTime));
        setPickupSlots(future);
        setSelectedSlotId(null);
      })
      .catch(() => setPickupSlots([]));
  }, [selectedStock?.location?.id]);

  const maxQty = selectedStock?.quantity ?? 0;
  const qtyNum = Number(quantity);
  const qtyError = quantity !== "" && (isNaN(qtyNum) || qtyNum <= 0 || qtyNum > maxQty)
    ? `Enter a value between 0 and ${maxQty}`
    : null;

  const handleSubmit = async () => {
    if (!selectedStock || !quantity || qtyError) return;
    setSubmitting(true);
    setError("");
    try {
      const token = getXsrfToken();
      const res = await fetch(apiUrl("/api/orders/place"), {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json", ...(token && { "X-XSRF-TOKEN": token }) },
        body: JSON.stringify({
          productId: product.id,
          locationId: selectedStock.location.id,
          quantity: qtyNum,
          pickupSlotId: selectedSlotId || null,
        }),
      });
      if (!res.ok) {
        const errorData = await parseErrorResponse(res);
        throw new Error(formatServerError(errorData, "Failed to place order"));
      }
      setSuccess(true);
      setTimeout(onSuccess, 1400);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  if (!product) return null;

  if (success) {
    return (
      <div style={{ textAlign: "center", padding: "32px 0" }}>
        <div style={{ fontSize: 40, marginBottom: 12 }}>✓</div>
        <div style={{ color: "#4caf50", fontSize: 16, fontFamily: "'DM Mono', monospace" }}>
          Order placed successfully!
        </div>
      </div>
    );
  }

  return (
    <>
      {error && (
        <div style={{ background: "#ff3b3b11", border: "1px solid #ff3b3b33", borderRadius: 7, padding: "10px 14px", marginBottom: 20, color: "#ff3b3b", fontSize: 13 }}>
          ⚠ {error}
        </div>
      )}

      {loading ? (
        <div style={{ color: "#555", fontSize: 13 }}>Loading available locations...</div>
      ) : stocks.length === 0 ? (
        <div style={{ color: "#555", fontSize: 13, textAlign: "center", padding: "24px 0" }}>
          This product is currently out of stock.
        </div>
      ) : (
        <>
          <Field label="Pickup Location" error={null}>
            <select
              value={selectedStock?.location?.id ?? ""}
              onChange={e => setSelectedStock(stocks.find(s => String(s.location?.id) === e.target.value) || null)}
              style={selectStyle}
            >
              {stocks.length > 1 && <option value="">Select a location...</option>}
              {stocks.map(s => (
                <option key={s.location?.id} value={s.location?.id}>
                  {s.location?.name}{s.location?.city ? ` — ${s.location.city}` : ""}
                  {" "}({s.quantity} {product.unit} available)
                </option>
              ))}
            </select>
          </Field>

          {selectedStock && (
            <>
              <Field label={`Quantity (max ${maxQty} ${product.unit})`} error={qtyError}>
                <input
                  type="number"
                  min="0.01"
                  max={maxQty}
                  step="0.01"
                  value={quantity}
                  onChange={e => setQuantity(e.target.value)}
                  placeholder={`0 – ${maxQty}`}
                  style={inputStyle(!!qtyError)}
                />
              </Field>

              {pickupSlots.length > 0 && (
                <div style={{ marginBottom: 18 }}>
                  <div style={{ fontSize: 10, letterSpacing: "0.14em", textTransform: "uppercase", color: "#555", marginBottom: 10, fontFamily: "'DM Mono', monospace" }}>
                    Pickup Time (optional)
                  </div>
                  <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                    {pickupSlots.map(slot => (
                      <label key={slot.id} style={{ display: "flex", alignItems: "center", gap: 10, padding: "8px 10px", background: selectedSlotId === slot.id ? "#1a1a2e" : "#111", border: `1px solid ${selectedSlotId === slot.id ? "#4a4aff55" : "#1e1e1e"}`, borderRadius: 6, cursor: "pointer" }}>
                        <input
                          type="radio"
                          name="pickupSlot"
                          value={slot.id}
                          checked={selectedSlotId === slot.id}
                          onChange={() => setSelectedSlotId(selectedSlotId === slot.id ? null : slot.id)}
                          style={{ accentColor: "#7777ff" }}
                        />
                        <div>
                          <div style={{ color: "#ccc", fontSize: 12 }}>{fmtDT(slot.startTime)} – {fmtDT(slot.endTime)}</div>
                          {slot.maxOrders != null && (
                            <div style={{ color: "#555", fontSize: 10 }}>max {slot.maxOrders} orders</div>
                          )}
                        </div>
                      </label>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}

          <button
            onClick={handleSubmit}
            disabled={submitting || !selectedStock || !quantity || !!qtyError}
            style={{
              width: "100%", padding: "13px 0", marginTop: 8,
              background: (submitting || !selectedStock || !quantity || !!qtyError) ? "#1e1e1e" : "#f0f0f0",
              color: (submitting || !selectedStock || !quantity || !!qtyError) ? "#555" : "#111",
              border: "none", borderRadius: 7, cursor: "pointer",
              fontFamily: "'DM Mono', monospace", fontSize: 13, fontWeight: 500,
              letterSpacing: "0.08em", transition: "background .2s",
            }}
          >
            {submitting ? "Placing order..." : "Place Order"}
          </button>
        </>
      )}
    </>
  );
}

// ── Order List Modal (shared by customer "My Orders" and vendor "Incoming Orders") ──

function OrderListModal({ fetchUrl, emptyMessage, canDeliver = false, getXsrfToken, parseErrorResponse, formatServerError }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [cancelling, setCancelling] = useState(null);
  const [delivering, setDelivering] = useState(null);
  const [ratingOpenId, setRatingOpenId] = useState(null);
  const [ratingScore, setRatingScore] = useState(0);
  const [ratingComment, setRatingComment] = useState("");
  const [submittingRating, setSubmittingRating] = useState(false);
  const [ratingError, setRatingError] = useState("");

  const fetchOrders = () => {
    setLoading(true);
    setError("");
    fetch(apiUrl(fetchUrl), { credentials: "include" })
      .then(r => { if (!r.ok) throw new Error("Failed to load orders"); return r.json(); })
      .then(setOrders)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchOrders(); }, [fetchUrl]);

  const handleCancel = async (orderId) => {
    if (!window.confirm("Cancel this order? Stock will be restored.")) return;
    setCancelling(orderId);
    setError("");
    try {
      const token = getXsrfToken();
      const res = await fetch(apiUrl(`/api/orders/${orderId}/cancel`), {
        method: "POST",
        credentials: "include",
        headers: token ? { "X-XSRF-TOKEN": token } : {},
      });
      if (!res.ok) {
        const errorData = await parseErrorResponse(res);
        throw new Error(formatServerError(errorData, "Failed to cancel order"));
      }
      fetchOrders();
    } catch (err) {
      setError(err.message);
    } finally {
      setCancelling(null);
    }
  };

  const handleDeliver = async (orderId) => {
    if (!window.confirm("Mark this order as delivered? Stock will not be returned.")) return;
    setDelivering(orderId);
    setError("");
    try {
      const token = getXsrfToken();
      const res = await fetch(apiUrl(`/api/orders/${orderId}/deliver`), {
        method: "POST",
        credentials: "include",
        headers: token ? { "X-XSRF-TOKEN": token } : {},
      });
      if (!res.ok) {
        const errorData = await parseErrorResponse(res);
        throw new Error(formatServerError(errorData, "Failed to mark order as delivered"));
      }
      fetchOrders();
    } catch (err) {
      setError(err.message);
    } finally {
      setDelivering(null);
    }
  };

  const handleRate = async (orderId) => {
    if (!ratingScore) { setRatingError("Please select a score."); return; }
    setSubmittingRating(true);
    setRatingError("");
    try {
      const token = getXsrfToken();
      const res = await fetch(apiUrl(`/api/orders/${orderId}/rate`), {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json", ...(token && { "X-XSRF-TOKEN": token }) },
        body: JSON.stringify({ score: ratingScore, comment: ratingComment || null }),
      });
      if (!res.ok) {
        const errorData = await parseErrorResponse(res);
        throw new Error(formatServerError(errorData, "Failed to submit rating"));
      }
      setRatingOpenId(null);
      setRatingScore(0);
      setRatingComment("");
      fetchOrders();
    } catch (err) {
      setRatingError(err.message);
    } finally {
      setSubmittingRating(false);
    }
  };

  const statusColor = { PENDING: "#f0f0f0", CONFIRMED: "#4caf50", PICKED_UP: "#2196f3", CANCELLED: "#555" };

  return (
    <>
      {error && (
        <div style={{ background: "#ff3b3b11", border: "1px solid #ff3b3b33", borderRadius: 7, padding: "10px 14px", marginBottom: 20, color: "#ff3b3b", fontSize: 13 }}>
          ⚠ {error}
        </div>
      )}

      {loading ? (
        <div style={{ color: "#555", fontSize: 13 }}>Loading...</div>
      ) : orders.length === 0 ? (
        <div style={{ color: "#444", fontSize: 13, textAlign: "center", padding: "24px 0" }}>{emptyMessage}</div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          {orders.map(order => (
            <div key={order.id} style={{ background: "#0d0d0d", border: "1px solid #1e1e1e", borderRadius: 8, padding: "14px 16px" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 10 }}>
                <div>
                  <span style={{ color: "#555", fontSize: 11, fontFamily: "'DM Mono', monospace" }}>
                    #{order.id} · {order.createdAt ? new Date(order.createdAt).toLocaleDateString() : ""}
                  </span>
                  <div style={{ color: "#aaa", fontSize: 12, marginTop: 3 }}>
                    {order.location?.name}{order.location?.city ? `, ${order.location.city}` : ""}
                  </div>
                  {order.pickupSlot && (
                    <div style={{ color: "#666", fontSize: 11, marginTop: 2, fontFamily: "'DM Mono', monospace" }}>
                      {new Date(order.pickupSlot.startTime).toLocaleString([], { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" })}
                      {" – "}
                      {new Date(order.pickupSlot.endTime).toLocaleString([], { hour: "2-digit", minute: "2-digit" })}
                    </div>
                  )}
                  {order.customer?.user?.username && (
                    <div style={{ color: "#666", fontSize: 11, marginTop: 2 }}>
                      Customer: {order.customer.user.username}
                    </div>
                  )}
                </div>
                <span style={{
                  fontSize: 10, letterSpacing: "0.1em", textTransform: "uppercase",
                  color: statusColor[order.status] || "#888", fontFamily: "'DM Mono', monospace",
                }}>
                  {order.status}
                </span>
              </div>

              {order.items?.map(item => (
                <div key={item.id} style={{ fontSize: 13, color: "#888", marginBottom: 4 }}>
                  {item.product?.name || "—"} × {item.quantity} {item.product?.unit || ""}
                </div>
              ))}

              {(order.status === "PENDING" || order.status === "CONFIRMED") && (
                <div style={{ display: "flex", gap: 8, marginTop: 10, flexWrap: "wrap" }}>
                  {canDeliver && (
                    <button
                      onClick={() => handleDeliver(order.id)}
                      disabled={delivering === order.id}
                      style={{
                        ...buttonStyle("primary"), fontSize: 11, padding: "5px 12px",
                        opacity: delivering === order.id ? 0.5 : 1,
                      }}
                    >
                      {delivering === order.id ? "Saving..." : "Mark as Delivered"}
                    </button>
                  )}
                  <button
                    onClick={() => handleCancel(order.id)}
                    disabled={cancelling === order.id}
                    style={{
                      ...buttonStyle("secondary"),
                      color: "#ff3b3b", borderColor: "#ff3b3b33", fontSize: 11, padding: "5px 12px",
                      opacity: cancelling === order.id ? 0.5 : 1,
                    }}
                  >
                    {cancelling === order.id ? "Cancelling..." : "Cancel Order"}
                  </button>
                </div>
              )}

              {order.status === "PICKED_UP" && (() => {
                const currentUserId = String(loadUser()?.id);
                const myRating = order.ratings?.find(r => String(r.rater?.id) === currentUserId);

                if (myRating) {
                  return (
                    <div style={{ marginTop: 12, paddingTop: 10, borderTop: "1px solid #1e1e1e" }}>
                      <div style={{ fontSize: 11, color: "#555", marginBottom: 4 }}>Your rating</div>
                      <div style={{ color: "#f5a623", fontSize: 18, letterSpacing: 2 }}>
                        {"★".repeat(myRating.score)}{"☆".repeat(5 - myRating.score)}
                      </div>
                      {myRating.comment && (
                        <div style={{ fontSize: 12, color: "#666", marginTop: 4 }}>{myRating.comment}</div>
                      )}
                    </div>
                  );
                }

                const isOpen = ratingOpenId === order.id;
                return (
                  <div style={{ marginTop: 12, paddingTop: 10, borderTop: "1px solid #1e1e1e" }}>
                    {!isOpen ? (
                      <button
                        onClick={() => { setRatingOpenId(order.id); setRatingScore(0); setRatingComment(""); setRatingError(""); }}
                        style={{ ...buttonStyle("secondary"), fontSize: 11, padding: "5px 12px" }}
                      >
                        Leave a Rating
                      </button>
                    ) : (
                      <div>
                        <div style={{ fontSize: 11, color: "#888", marginBottom: 8 }}>Rate this order</div>
                        <div style={{ display: "flex", gap: 2, marginBottom: 10 }}>
                          {[1, 2, 3, 4, 5].map(n => (
                            <button
                              key={n}
                              onClick={() => setRatingScore(n)}
                              style={{
                                background: "none", border: "none", cursor: "pointer",
                                fontSize: 26, color: n <= ratingScore ? "#f5a623" : "#333",
                                padding: "0 2px", lineHeight: 1,
                              }}
                            >
                              {n <= ratingScore ? "★" : "☆"}
                            </button>
                          ))}
                        </div>
                        <textarea
                          value={ratingComment}
                          onChange={e => setRatingComment(e.target.value)}
                          placeholder="Optional comment..."
                          rows={2}
                          style={{
                            width: "100%", background: "#111", border: "1px solid #2a2a2a",
                            borderRadius: 5, color: "#ccc", fontSize: 12, padding: "6px 8px",
                            resize: "none", fontFamily: "inherit", boxSizing: "border-box",
                          }}
                        />
                        {ratingError && (
                          <div style={{ color: "#ff3b3b", fontSize: 11, marginTop: 4 }}>{ratingError}</div>
                        )}
                        <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
                          <button
                            onClick={() => handleRate(order.id)}
                            disabled={submittingRating}
                            style={{ ...buttonStyle("primary"), fontSize: 11, padding: "5px 14px", opacity: submittingRating ? 0.5 : 1 }}
                          >
                            {submittingRating ? "Submitting..." : "Submit"}
                          </button>
                          <button
                            onClick={() => { setRatingOpenId(null); setRatingError(""); }}
                            style={{ ...buttonStyle("secondary"), fontSize: 11, padding: "5px 12px" }}
                          >
                            Cancel
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })()}
            </div>
          ))}
        </div>
      )}
    </>
  );
}

function LocationsModalContent({ currentVendor, onLocationsChanged, getXsrfToken, parseErrorResponse, formatServerError }) {
  const emptyForm = { name: "", address: "", city: "", latitude: "", longitude: "" };
  const emptySlotForm = { startTime: "", endTime: "", maxOrders: "" };
  const [form, setForm] = useState(emptyForm);
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [editingId, setEditingId] = useState(null);
  const [editForm, setEditForm] = useState(emptyForm);
  const [slotsOpenId, setSlotsOpenId] = useState(null);
  const [slotForm, setSlotForm] = useState(emptySlotForm);
  const [slotError, setSlotError] = useState("");
  const [submittingSlot, setSubmittingSlot] = useState(false);

  const fmtDT = (dt) => dt ? new Date(dt).toLocaleString([], { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" }) : "";
  const toServerDT = (v) => v.length === 16 ? v + ":00" : v;

  const handleAddSlot = async (locationId) => {
    if (!slotForm.startTime || !slotForm.endTime) { setSlotError("Start and end time are required."); return; }
    if (slotForm.startTime >= slotForm.endTime) { setSlotError("End time must be after start time."); return; }
    setSubmittingSlot(true);
    setSlotError("");
    try {
      const token = getXsrfToken();
      const body = {
        location: { id: locationId },
        startTime: toServerDT(slotForm.startTime),
        endTime: toServerDT(slotForm.endTime),
        maxOrders: slotForm.maxOrders !== "" ? Number(slotForm.maxOrders) : null,
      };
      const res = await fetch(apiUrl("/api/pickup-slots"), {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json", ...(token && { "X-XSRF-TOKEN": token }) },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        const errorData = await parseErrorResponse(res);
        throw new Error(formatServerError(errorData, "Failed to add slot"));
      }
      setSlotForm(emptySlotForm);
      onLocationsChanged();
    } catch (err) {
      setSlotError(err.message);
    } finally {
      setSubmittingSlot(false);
    }
  };

  const handleDeleteSlot = async (slotId) => {
    if (!window.confirm("Delete this pickup slot?")) return;
    setSlotError("");
    try {
      const token = getXsrfToken();
      const res = await fetch(apiUrl(`/api/pickup-slots/${slotId}`), {
        method: "DELETE",
        credentials: "include",
        headers: token ? { "X-XSRF-TOKEN": token } : {},
      });
      if (!res.ok) throw new Error("Failed to delete slot");
      onLocationsChanged();
    } catch (err) {
      setSlotError(err.message);
    }
  };

  const locations = currentVendor?.locations || [];

  const validateForm = (data) => {
    const errors = {};
    if (!data.name.trim()) errors.name = "Name is required";
    if (data.latitude !== "" && isNaN(Number(data.latitude))) errors.latitude = "Must be a number";
    if (data.longitude !== "" && isNaN(Number(data.longitude))) errors.longitude = "Must be a number";
    return errors;
  };

  const handleAdd = async () => {
    const errors = validateForm(form);
    setFormErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);
    setError("");
    try {
      const token = getXsrfToken();
      const body = {
        name: form.name.trim(),
        address: form.address.trim() || null,
        city: form.city.trim() || null,
        latitude: form.latitude !== "" ? Number(form.latitude) : null,
        longitude: form.longitude !== "" ? Number(form.longitude) : null,
      };
      const res = await fetch(apiUrl("/api/locations"), {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json", ...(token && { "X-XSRF-TOKEN": token }) },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        const errorData = await parseErrorResponse(res);
        throw new Error(formatServerError(errorData, "Failed to create location"));
      }
      setForm(emptyForm);
      setFormErrors({});
      onLocationsChanged();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const startEdit = (loc) => {
    setEditingId(loc.id);
    setEditForm({
      name: loc.name || "",
      address: loc.address || "",
      city: loc.city || "",
      latitude: loc.latitude != null ? String(loc.latitude) : "",
      longitude: loc.longitude != null ? String(loc.longitude) : "",
    });
  };

  const handleSaveEdit = async (locId) => {
    const errors = validateForm(editForm);
    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);
    setError("");
    try {
      const token = getXsrfToken();
      const body = {
        name: editForm.name.trim(),
        address: editForm.address.trim() || null,
        city: editForm.city.trim() || null,
        latitude: editForm.latitude !== "" ? Number(editForm.latitude) : null,
        longitude: editForm.longitude !== "" ? Number(editForm.longitude) : null,
      };
      const res = await fetch(apiUrl(`/api/locations/${locId}`), {
        method: "PUT",
        credentials: "include",
        headers: { "Content-Type": "application/json", ...(token && { "X-XSRF-TOKEN": token }) },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        const errorData = await parseErrorResponse(res);
        throw new Error(formatServerError(errorData, "Failed to update location"));
      }
      setEditingId(null);
      onLocationsChanged();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (locId) => {
    if (!window.confirm("Delete this location? Stock entries linked to it will also be removed.")) return;
    setError("");
    try {
      const token = getXsrfToken();
      const res = await fetch(apiUrl(`/api/locations/${locId}`), {
        method: "DELETE",
        credentials: "include",
        headers: token ? { "X-XSRF-TOKEN": token } : {},
      });
      if (!res.ok) throw new Error("Failed to delete location");
      onLocationsChanged();
    } catch (err) {
      setError(err.message);
    }
  };

  const rowStyle = {
    background: "#0d0d0d", border: "1px solid #1e1e1e",
    borderRadius: 8, padding: "14px 16px", marginBottom: 10,
  };
  const labelStyle = {
    fontSize: 10, letterSpacing: "0.14em", textTransform: "uppercase",
    color: "#555", marginBottom: 12, fontFamily: "'DM Mono', monospace",
    display: "block",
  };

  return (
    <>
      {error && (
        <div style={{
          background: "#ff3b3b11", border: "1px solid #ff3b3b33", borderRadius: 7,
          padding: "10px 14px", marginBottom: 20, color: "#ff3b3b", fontSize: 13,
        }}>⚠ {error}</div>
      )}

      {/* Existing locations */}
      <div style={{ marginBottom: 28 }}>
        <span style={labelStyle}>Your Locations</span>

        {locations.length === 0 ? (
          <div style={{ color: "#444", fontSize: 13, padding: "8px 0" }}>No locations yet.</div>
        ) : locations.map(loc => (
          <div key={loc.id} style={rowStyle}>
            {editingId === loc.id ? (
              <>
                <Field label="Name" error={null}>
                  <input value={editForm.name} onChange={e => setEditForm(p => ({ ...p, name: e.target.value }))} style={inputStyle(false)} />
                </Field>
                <Field label="Address" error={null}>
                  <input value={editForm.address} onChange={e => setEditForm(p => ({ ...p, address: e.target.value }))} style={inputStyle(false)} />
                </Field>
                <Field label="City" error={null}>
                  <input value={editForm.city} onChange={e => setEditForm(p => ({ ...p, city: e.target.value }))} style={inputStyle(false)} />
                </Field>
                <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
                  <div style={{ flex: 1 }}>
                    <Field label="Latitude" error={null}>
                      <input type="number" step="any" value={editForm.latitude} onChange={e => setEditForm(p => ({ ...p, latitude: e.target.value }))} style={inputStyle(false)} />
                    </Field>
                  </div>
                  <div style={{ flex: 1 }}>
                    <Field label="Longitude" error={null}>
                      <input type="number" step="any" value={editForm.longitude} onChange={e => setEditForm(p => ({ ...p, longitude: e.target.value }))} style={inputStyle(false)} />
                    </Field>
                  </div>
                </div>
                <div style={{ display: "flex", gap: 8 }}>
                  <button onClick={() => handleSaveEdit(loc.id)} disabled={submitting} style={{ ...buttonStyle("primary"), flex: 1, padding: "8px" }}>
                    Save
                  </button>
                  <button onClick={() => setEditingId(null)} style={{ ...buttonStyle("secondary"), flex: 1, padding: "8px" }}>
                    Cancel
                  </button>
                </div>
              </>
            ) : (
              <div>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12 }}>
                  <div style={{ minWidth: 0 }}>
                    <div style={{ color: "#f0f0f0", fontSize: 14, fontWeight: 500 }}>{loc.name}</div>
                    {(loc.address || loc.city) && (
                      <div style={{ color: "#666", fontSize: 12, marginTop: 3 }}>
                        {[loc.address, loc.city].filter(Boolean).join(", ")}
                      </div>
                    )}
                  </div>
                  <div style={{ display: "flex", gap: 8, flexShrink: 0 }}>
                    <button
                      onClick={() => { setSlotsOpenId(p => p === loc.id ? null : loc.id); setSlotForm(emptySlotForm); setSlotError(""); }}
                      style={{ ...buttonStyle("secondary"), padding: "5px 10px", fontSize: 11 }}
                    >
                      Slots ({loc.pickupSlots?.length || 0})
                    </button>
                    <button onClick={() => startEdit(loc)} style={{ ...buttonStyle("secondary"), padding: "5px 10px", fontSize: 11 }}>Edit</button>
                    <button onClick={() => handleDelete(loc.id)} style={{ ...buttonStyle("secondary"), padding: "5px 10px", fontSize: 11, color: "#ff3b3b", borderColor: "#ff3b3b33" }}>Delete</button>
                  </div>
                </div>

                {slotsOpenId === loc.id && (
                  <div style={{ marginTop: 14, borderTop: "1px solid #1e1e1e", paddingTop: 14 }}>
                    <div style={{ fontSize: 11, letterSpacing: "0.12em", textTransform: "uppercase", color: "#555", marginBottom: 10, fontFamily: "'DM Mono', monospace" }}>
                      Pickup Slots
                    </div>

                    {slotError && (
                      <div style={{ color: "#ff3b3b", fontSize: 11, marginBottom: 10 }}>⚠ {slotError}</div>
                    )}

                    {(loc.pickupSlots?.length ?? 0) === 0 ? (
                      <div style={{ color: "#444", fontSize: 12, marginBottom: 12 }}>No slots yet.</div>
                    ) : (
                      <div style={{ marginBottom: 14 }}>
                        {loc.pickupSlots.map(slot => (
                          <div key={slot.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "7px 10px", background: "#111", borderRadius: 6, marginBottom: 6 }}>
                            <div>
                              <span style={{ color: "#ccc", fontSize: 12 }}>{fmtDT(slot.startTime)} – {fmtDT(slot.endTime)}</span>
                              {slot.maxOrders != null && (
                                <span style={{ color: "#555", fontSize: 11, marginLeft: 8 }}>max {slot.maxOrders}</span>
                              )}
                            </div>
                            <button
                              onClick={() => handleDeleteSlot(slot.id)}
                              style={{ ...buttonStyle("secondary"), padding: "3px 8px", fontSize: 10, color: "#ff3b3b", borderColor: "#ff3b3b33" }}
                            >
                              Delete
                            </button>
                          </div>
                        ))}
                      </div>
                    )}

                    <div style={{ display: "flex", gap: 8, alignItems: "flex-end", flexWrap: "wrap" }}>
                      <div style={{ flex: "1 1 140px" }}>
                        <div style={{ fontSize: 10, color: "#555", marginBottom: 4, textTransform: "uppercase", letterSpacing: "0.1em" }}>Start</div>
                        <input
                          type="datetime-local"
                          value={slotForm.startTime}
                          onChange={e => setSlotForm(p => ({ ...p, startTime: e.target.value }))}
                          style={{ ...inputStyle(false), colorScheme: "dark" }}
                        />
                      </div>
                      <div style={{ flex: "1 1 140px" }}>
                        <div style={{ fontSize: 10, color: "#555", marginBottom: 4, textTransform: "uppercase", letterSpacing: "0.1em" }}>End</div>
                        <input
                          type="datetime-local"
                          value={slotForm.endTime}
                          onChange={e => setSlotForm(p => ({ ...p, endTime: e.target.value }))}
                          style={{ ...inputStyle(false), colorScheme: "dark" }}
                        />
                      </div>
                      <div style={{ flex: "0 1 90px" }}>
                        <div style={{ fontSize: 10, color: "#555", marginBottom: 4, textTransform: "uppercase", letterSpacing: "0.1em" }}>Max orders</div>
                        <input
                          type="number"
                          min="1"
                          value={slotForm.maxOrders}
                          onChange={e => setSlotForm(p => ({ ...p, maxOrders: e.target.value }))}
                          placeholder="∞"
                          style={{ ...inputStyle(false) }}
                        />
                      </div>
                      <button
                        onClick={() => handleAddSlot(loc.id)}
                        disabled={submittingSlot}
                        style={{ ...buttonStyle("primary"), padding: "8px 14px", fontSize: 11, flexShrink: 0, opacity: submittingSlot ? 0.5 : 1 }}
                      >
                        {submittingSlot ? "Adding..." : "+ Add Slot"}
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Add new location */}
      <div style={{ height: 1, background: "#1e1e1e", marginBottom: 24 }} />
      <span style={labelStyle}>Add Location</span>

      <Field label="Name *" error={formErrors.name}>
        <input
          value={form.name}
          onChange={e => setForm(p => ({ ...p, name: e.target.value }))}
          placeholder="Main Farm Stand"
          style={inputStyle(!!formErrors.name)}
        />
      </Field>
      <Field label="Address" error={null}>
        <input
          value={form.address}
          onChange={e => setForm(p => ({ ...p, address: e.target.value }))}
          placeholder="123 Main St"
          style={inputStyle(false)}
        />
      </Field>
      <Field label="City" error={null}>
        <input
          value={form.city}
          onChange={e => setForm(p => ({ ...p, city: e.target.value }))}
          placeholder="Bucharest"
          style={inputStyle(false)}
        />
      </Field>
      <div style={{ display: "flex", gap: 12 }}>
        <div style={{ flex: 1 }}>
          <Field label="Latitude" error={formErrors.latitude}>
            <input
              type="number" step="any"
              value={form.latitude}
              onChange={e => setForm(p => ({ ...p, latitude: e.target.value }))}
              placeholder="44.4268"
              style={inputStyle(!!formErrors.latitude)}
            />
          </Field>
        </div>
        <div style={{ flex: 1 }}>
          <Field label="Longitude" error={formErrors.longitude}>
            <input
              type="number" step="any"
              value={form.longitude}
              onChange={e => setForm(p => ({ ...p, longitude: e.target.value }))}
              placeholder="26.1025"
              style={inputStyle(!!formErrors.longitude)}
            />
          </Field>
        </div>
      </div>

      <button
        onClick={handleAdd}
        disabled={submitting}
        style={{
          width: "100%", padding: "12px 0",
          background: submitting ? "#1e1e1e" : "#f0f0f0",
          color: submitting ? "#555" : "#111",
          border: "none", borderRadius: 7, cursor: submitting ? "not-allowed" : "pointer",
          fontFamily: "'DM Mono', monospace", fontSize: 13, fontWeight: 500,
          letterSpacing: "0.08em", transition: "background .2s, color .2s",
        }}
      >
        {submitting ? "Processing..." : "Add Location"}
      </button>
    </>
  );
}

function StockModalContent({ product, currentVendor, getXsrfToken, parseErrorResponse, formatServerError }) {
  const [stocks, setStocks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [form, setForm] = useState({ locationId: "", quantity: "" });
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [editingQuantity, setEditingQuantity] = useState("");

  const vendorLocationIds = new Set((currentVendor?.locations || []).map(l => l.id));

  const fetchStocks = async () => {
    setLoading(true);
    setError("");
    try {
      const res = await fetch(apiUrl(`/api/stocks?productId=${product.id}`), { credentials: "include" });
      if (!res.ok) throw new Error("Failed to fetch stocks");
      const data = await res.json();
      setStocks(data.filter(s => vendorLocationIds.has(s.location?.id)));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (product) fetchStocks();
  }, [product?.id]);

  const usedLocationIds = new Set(stocks.map(s => s.location?.id));
  const availableLocations = (currentVendor?.locations || []).filter(l => !usedLocationIds.has(l.id));

  const validateForm = () => {
    const errors = {};
    if (!form.locationId) errors.locationId = "Location is required";
    if (form.quantity === "") errors.quantity = "Quantity is required";
    else if (isNaN(Number(form.quantity)) || Number(form.quantity) < 0) errors.quantity = "Must be a non-negative number";
    return errors;
  };

  const handleAdd = async () => {
    const errors = validateForm();
    setFormErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);
    try {
      const token = getXsrfToken();
      const res = await fetch(apiUrl("/api/stocks"), {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json", ...(token && { "X-XSRF-TOKEN": token }) },
        body: JSON.stringify({
          product: { id: product.id },
          location: { id: Number(form.locationId) },
          quantity: Number(form.quantity),
        }),
      });
      if (!res.ok) {
        const errorData = await parseErrorResponse(res);
        throw new Error(formatServerError(errorData, "Failed to add stock entry"));
      }
      setForm({ locationId: "", quantity: "" });
      setFormErrors({});
      fetchStocks();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleSaveEdit = async (stock) => {
    if (editingQuantity === "" || isNaN(Number(editingQuantity)) || Number(editingQuantity) < 0) return;
    setSubmitting(true);
    try {
      const token = getXsrfToken();
      const res = await fetch(apiUrl(`/api/stocks/${stock.id}`), {
        method: "PUT",
        credentials: "include",
        headers: { "Content-Type": "application/json", ...(token && { "X-XSRF-TOKEN": token }) },
        body: JSON.stringify({
          product: { id: product.id },
          location: { id: stock.location?.id },
          quantity: Number(editingQuantity),
        }),
      });
      if (!res.ok) {
        const errorData = await parseErrorResponse(res);
        throw new Error(formatServerError(errorData, "Failed to update stock"));
      }
      setEditingId(null);
      setEditingQuantity("");
      fetchStocks();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (stockId) => {
    if (!window.confirm("Remove this stock entry?")) return;
    try {
      const token = getXsrfToken();
      const res = await fetch(apiUrl(`/api/stocks/${stockId}`), {
        method: "DELETE",
        credentials: "include",
        headers: token ? { "X-XSRF-TOKEN": token } : {},
      });
      if (!res.ok) throw new Error("Failed to delete stock entry");
      fetchStocks();
    } catch (err) {
      setError(err.message);
    }
  };

  if (!product) return null;

  const vendorLocations = currentVendor?.locations || [];

  return (
    <>
      {error && (
        <div style={{
          background: "#ff3b3b11", border: "1px solid #ff3b3b33", borderRadius: 7,
          padding: "10px 14px", marginBottom: 20, color: "#ff3b3b", fontSize: 13,
        }}>⚠ {error}</div>
      )}

      {/* Existing stock entries */}
      <div style={{ marginBottom: 28 }}>
        <div style={{
          fontSize: 10, letterSpacing: "0.14em", textTransform: "uppercase",
          color: "#555", marginBottom: 12, fontFamily: "'DM Mono', monospace",
        }}>Current Stock</div>

        {loading ? (
          <div style={{ color: "#555", fontSize: 13, padding: "8px 0" }}>Loading...</div>
        ) : stocks.length === 0 ? (
          <div style={{ color: "#444", fontSize: 13, padding: "8px 0" }}>No stock entries yet.</div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
            {stocks.map(stock => (
              <div key={stock.id} style={{
                background: "#0d0d0d", border: "1px solid #1e1e1e", borderRadius: 8,
                padding: "12px 16px", display: "flex", alignItems: "center",
                justifyContent: "space-between", gap: 12,
              }}>
                <div style={{ color: "#aaa", fontSize: 13, flex: 1, minWidth: 0 }}>
                  {stock.location?.name || `Location #${stock.location?.id}`}
                </div>

                {editingId === stock.id ? (
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <input
                      type="number" min="0" step="0.01"
                      value={editingQuantity}
                      onChange={e => setEditingQuantity(e.target.value)}
                      style={{ ...inputStyle(false), width: 90, padding: "6px 10px" }}
                      autoFocus
                    />
                    <button
                      onClick={() => handleSaveEdit(stock)}
                      disabled={submitting}
                      style={{ ...buttonStyle("primary"), padding: "6px 12px", fontSize: 11 }}
                    >Save</button>
                    <button
                      onClick={() => { setEditingId(null); setEditingQuantity(""); }}
                      style={{ ...buttonStyle("secondary"), padding: "6px 12px", fontSize: 11 }}
                    >Cancel</button>
                  </div>
                ) : (
                  <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                    <span style={{ color: "#f0f0f0", fontSize: 14, fontWeight: 500, minWidth: 56, textAlign: "right" }}>
                      {stock.quantity}
                    </span>
                    <button
                      onClick={() => { setEditingId(stock.id); setEditingQuantity(String(stock.quantity)); }}
                      style={{ ...buttonStyle("secondary"), padding: "5px 10px", fontSize: 11 }}
                    >Edit</button>
                    <button
                      onClick={() => handleDelete(stock.id)}
                      style={{ ...buttonStyle("secondary"), padding: "5px 10px", fontSize: 11, color: "#ff3b3b", borderColor: "#ff3b3b33" }}
                    >Remove</button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Add new stock entry */}
      {!loading && vendorLocations.length === 0 && (
        <div style={{ color: "#555", fontSize: 13, textAlign: "center" }}>
          You have no locations set up. Add locations before managing stock.
        </div>
      )}

      {!loading && availableLocations.length > 0 && (
        <>
          <div style={{ height: 1, background: "#1e1e1e", marginBottom: 24 }} />
          <div style={{
            fontSize: 10, letterSpacing: "0.14em", textTransform: "uppercase",
            color: "#555", marginBottom: 16, fontFamily: "'DM Mono', monospace",
          }}>Add Stock Entry</div>

          <Field label="Location" error={formErrors.locationId}>
            <select
              value={form.locationId}
              onChange={e => setForm(prev => ({ ...prev, locationId: e.target.value }))}
              style={selectStyle}
            >
              <option value="">Select location...</option>
              {availableLocations.map(loc => (
                <option key={loc.id} value={loc.id}>{loc.name}</option>
              ))}
            </select>
          </Field>

          <Field label="Quantity" error={formErrors.quantity}>
            <input
              type="number" min="0" step="0.01"
              value={form.quantity}
              onChange={e => setForm(prev => ({ ...prev, quantity: e.target.value }))}
              placeholder="0"
              style={inputStyle(!!formErrors.quantity)}
            />
          </Field>

          <button
            onClick={handleAdd}
            disabled={submitting}
            style={{
              width: "100%", padding: "12px 0",
              background: submitting ? "#1e1e1e" : "#f0f0f0",
              color: submitting ? "#555" : "#111",
              border: "none", borderRadius: 7, cursor: submitting ? "not-allowed" : "pointer",
              fontFamily: "'DM Mono', monospace", fontSize: 13, fontWeight: 500,
              letterSpacing: "0.08em", transition: "background .2s, color .2s",
            }}
          >
            {submitting ? "Processing..." : "Add Stock Entry"}
          </button>
        </>
      )}

      {!loading && availableLocations.length === 0 && stocks.length > 0 && (
        <div style={{ color: "#444", fontSize: 12, textAlign: "center", marginTop: 8 }}>
          All your locations already have stock entries for this product.
        </div>
      )}
    </>
  );
}

function ProductForm({ formData, formErrors, categories, onChange, onBlur, onSubmit, submitting, submitText }) {
  return (
    <>
      <Field label="Product Name" error={formErrors.name}>
        <input
          style={inputStyle(!!formErrors.name)}
          value={formData.name}
          onChange={e => onChange("name", e.target.value)}
          onBlur={() => onBlur("name")}
          placeholder="Organic Apples"
        />
      </Field>

      <Field label="Description" error={formErrors.description}>
        <textarea
          style={{ ...inputStyle(!!formErrors.description), minHeight: 80, resize: "vertical" }}
          value={formData.description}
          onChange={e => onChange("description", e.target.value)}
          onBlur={() => onBlur("description")}
          placeholder="Fresh organic apples from local farms..."
        />
      </Field>

      <Field label="Unit" error={formErrors.unit}>
        <input
          style={inputStyle(!!formErrors.unit)}
          value={formData.unit}
          onChange={e => onChange("unit", e.target.value)}
          onBlur={() => onBlur("unit")}
          placeholder="kg, piece, box, etc."
        />
      </Field>

      <Field label="Category" error={formErrors.category}>
        <select
          value={formData.category}
          onChange={e => onChange("category", e.target.value)}
          onBlur={() => onBlur("category")}
          style={selectStyle}
        >
          {categories.map(cat => (
            <option key={cat} value={cat}>{cat}</option>
          ))}
        </select>
      </Field>

      <Field label="Image URL (optional)" error={formErrors.imageUrl}>
        <input
          style={inputStyle(!!formErrors.imageUrl)}
          value={formData.imageUrl}
          onChange={e => onChange("imageUrl", e.target.value)}
          placeholder="https://example.com/image.jpg"
        />
      </Field>

      <button
        onClick={onSubmit}
        disabled={submitting}
        style={{
          width: "100%", marginTop: 8, padding: "13px 0",
          background: submitting ? "#1e1e1e" : "#f0f0f0",
          color: submitting ? "#555" : "#111",
          border: "none", borderRadius: 7, cursor: submitting ? "not-allowed" : "pointer",
          fontFamily: "'DM Mono', monospace", fontSize: 13, fontWeight: 500,
          letterSpacing: "0.08em", transition: "background .2s, color .2s",
        }}
      >
        {submitting ? "Processing..." : submitText}
      </button>
    </>
  );
}
