import { useState, useEffect } from "react";

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

  // Fetch current vendor info
  const fetchCurrentVendor = async () => {
    try {
      var user = loadUser();
      
      return user;
    } catch (err) {
      console.error("Failed to fetch vendor:", err);
      setIsVendor(false);
    }
  };

  const fetchProducts = async () => {
    setLoading(true);
    setError("");
    
    try {
      let url = `http://localhost:8080/api/products/paged?page=${page}&size=${size}&sortBy=${sortBy}&sortDir=${sortDir}`;
      if (categoryFilter) {
        url += `&category=${categoryFilter}`;
      }

      const res = await fetch(url, {
        method: "GET",
        credentials: "include",
      });
      
console.log(res);

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
      
      const productToCreate = {
        name: formData.name,
        description: formData.description,
        unit: formData.unit,
        category: formData.category,
        imageUrl: formData.imageUrl,
        vendor: loadUser().id 
      };
      
      const res = await fetch("http://localhost:8080/api/products", {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          ...(token && { "X-XSRF-TOKEN": token }),
        },
        body: JSON.stringify(productToCreate),
      });
      
      if (!res.ok) {
        const errorData = await res.json();
        throw new Error(errorData.message || "Failed to create product");
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
      
      const productToUpdate = {
        name: formData.name,
        description: formData.description,
        unit: formData.unit,
        category: formData.category,
        imageUrl: formData.imageUrl,
        vendor: { id: currentVendor.id }  // Keep using current vendor's ID
      };
      
      const res = await fetch(`http://localhost:8080/api/products/${selectedProduct.id}`, {
        method: "PUT",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          ...(token && { "X-XSRF-TOKEN": token }),
        },
        body: JSON.stringify(productToUpdate),
      });
      
      if (!res.ok) {
        const errorData = await res.json();
        throw new Error(errorData.message || "Failed to update product");
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
      const res = await fetch(`http://localhost:8080/api/products/${id}`, {
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
                ✓ Vendor: {currentVendor.name}
              </div>
            )}
          </div>
          <div style={{ display: "flex", gap: 12 }}>
            {/* Only show New Product button for vendors */}
            {loadUser().role == "VENDOR" && (
              <button onClick={() => setIsCreateModalOpen(true)} style={buttonStyle("primary")}>
                + New Product
              </button>
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
                  <div style={{ display: "flex", gap: 12, borderTop: "1px solid #1e1e1e", paddingTop: 16 }}>
                    <button onClick={() => openEditModal(product)} style={{ ...buttonStyle("secondary"), flex: 1, padding: "8px" }}>
                      Edit
                    </button>
                    <button onClick={() => handleDelete(product.id)} style={{ ...buttonStyle("secondary"), flex: 1, padding: "8px", color: "#ff3b3b", borderColor: "#ff3b3b33" }}>
                      Delete
                    </button>
                  </div>
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
    </div>
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