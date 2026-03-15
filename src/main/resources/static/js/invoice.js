let catalogItems = [];

function loadItems() {
    fetch("/items")
        .then(response => response.json())
        .then(items => {
            catalogItems = items;
            renderItemRows();
        })
        .catch(error => console.log('error', error));
}

function renderItemRows() {
    const tableBody = document.getElementById("invoice_items_table");
    while (tableBody.rows.length > 0) {
        tableBody.deleteRow(0);
    }
    catalogItems.forEach(item => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${item.name}</td>
            <td>${item.price}</td>
            <td>
                <button class="btn-small" onclick="changeQty('${item.id}', -1)">-</button>
                <span id="qty_${item.id}" style="margin:0 10px;">0</span>
                <button class="btn-small" onclick="changeQty('${item.id}', 1)">+</button>
            </td>
        `;
        tableBody.appendChild(tr);
    });
}

function changeQty(itemId, delta) {
    const span = document.getElementById("qty_" + itemId);
    let current = parseInt(span.innerText || "0", 10);
    current += delta;
    if (current < 0) current = 0;
    span.innerText = String(current);
    updateInvoiceTotal();
}

function updateInvoiceTotal() {
    let total = 0;
    catalogItems.forEach(item => {
        const span = document.getElementById("qty_" + item.id);
        if (!span) return;
        const qty = parseInt(span.innerText || "0", 10);
        if (qty > 0) {
            total += item.price * qty;
        }
    });
    document.getElementById("invoice_total").value = total.toFixed(2);
}

function generateInvoice() {
    let items = [];
    catalogItems.forEach(item => {
        const span = document.getElementById("qty_" + item.id);
        if (!span) return;
        const qty = parseInt(span.innerText || "0", 10);
        if (qty > 0) {
            items.push({ itemId: item.id, quantity: qty });
        }
    });

    if (items.length === 0) {
        alert("Please select at least one item.");
        return;
    }

    const total = document.getElementById("invoice_total").value;
    if (!total || parseFloat(total) <= 0) {
        alert("Total must be greater than 0.");
        return;
    }

    // Merchant details and UPI id should be known in this page.
    // For simplicity, prompt the merchant for name and UPI.
    const merchantName = prompt("Enter merchant name:");
    const upiId = prompt("Enter merchant UPI ID (e.g. name@upi):");
    if (!merchantName || !upiId) {
        alert("Merchant name and UPI ID are required.");
        return;
    }

    const payload = {
        merchantName: merchantName,
        upiId: upiId,
        items: items
    };

    fetch("/invoice", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
    })
        .then(response => response.text())
        .then(transactionId => {
            if (!transactionId) {
                alert("Failed to create invoice. Please try again.");
                return;
            }
            window.location.href = "/invoice/" + transactionId;
        })
        .catch(error => console.log('error', error));
}

document.addEventListener("DOMContentLoaded", function() {
    loadItems();
});


