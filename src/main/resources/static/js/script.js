
//global variables
var userObj = {};
var qrTimerInterval = null;
var catalogItemsForBill = [];
var currencyEnum = {
    "ILS":"&#8362;",
    "USD":"&#36;",
    "EUR":"&#128;",
    "INR":"&#8377;"
}
//global functions
function setUser(user) {
    userObj = user;
}

function setUsername() {
    document.getElementById("username_nav").innerText = userObj.name;
}

function navigate(show, hide) {
    //this function suppose to hide and show pages.
    //the function gets an id of the page to show and the id of the page to hide.
    document.getElementById(show).style = "display:block";
    document.getElementById(hide).style = "display:none";

    if (show === "login_page")
        document.getElementById("main").style = "display:none";
    else {
        setUsername();
        document.getElementById("main").style = "display:block";
    }
}

/*Login Page Functions */
function focusInChangeColor(e) {
    document.getElementById(e.id + "_div").classList.add("after-color")
}

function focusOutChangeColor(e) {
    document.getElementById(e.id + "_div").classList.remove("after-color")
}

function submitLogin() {
    const EMAIL = document.getElementById("email").value;
    const PASSWORD = document.getElementById("password").value;
    const UPI_ID = document.getElementById("upiId").value;

    var myHeaders = new Headers();
    myHeaders.append("Content-Type", "application/json");

    // use the values entered in the login form
    var raw = JSON.stringify({ "email": EMAIL, "password": PASSWORD });

    var requestOptions = {
        method: 'POST',
        headers: myHeaders,
        body: raw,
        redirect: 'follow'
    };

    // call local Spring Boot backend instead of the old Azure URL
    fetch("/merchantLogin", requestOptions)
        .then(response => {
            if (response.ok) return response.text();
            else handleError(error);
        })
        .then(result => {
            const user = JSON.parse(result);
            // attach the merchant UPI ID from the login form so it can be used for QR generation
            user.upiId = UPI_ID;
            setUser(user);
            resetLogin();
            navigate("home_page", "login_page");
        })
        .catch(error => {
            handleError(error)
        });
}

function resetLogin() {
    document.getElementById("email").value = "";
    document.getElementById("password").value = "";
    document.getElementById("upiId").value = "";
    document.getElementById("error-label-form").classList.remove("has-error");
}

function handleError(error) {
    document.getElementById("error-label-form").classList.add("has-error");
}

function navigateHomePage() {
    document.getElementById("username_nav").value = userObj.name;
}

function setActiveTab(e) {
    let activeElemArr = document.body.getElementsByClassName("active");
    activeElemArr[0].classList.remove('active');
    e.classList.add('active');
}

// load catalog when bill tab is first opened
document.addEventListener("DOMContentLoaded", function () {
    // optional: preload catalog so bill tab is instant
    loadCatalogForBill();
});

function generateQR() {
    var myHeaders = new Headers();
    // send plain text UPI string
    myHeaders.append("Content-Type", "text/plain");

    const amount = document.getElementById("price").value;
    const upiId = userObj.upiId;

    // Build standard UPI payment URI: upi://pay?pa=<upiId>&pn=<merchantName>&am=<amount>&cu=INR
    const upiString =
        "upi://pay?pa=" + encodeURIComponent(upiId) +
        "&pn=" + encodeURIComponent(userObj.name) +
        "&am=" + encodeURIComponent(amount) +
        "&cu=" + encodeURIComponent("INR");

    var requestOptions = {
        method: 'POST',
        headers: myHeaders,
        body: upiString,
        redirect: 'follow'
    };

    // call local Spring Boot backend instead of the old Azure URL
    fetch("/generateQR", requestOptions)
        .then(response => response.blob())
        .then(blob => {
            var imageUrl = URL.createObjectURL(blob)
            document.getElementById("QR_image").src = imageUrl;

            // Start 2-minute countdown timer for this QR
            startQrTimer();
        })
        .catch(error => console.log('error', error));
}

function startQrTimer() {
    const qrTimerLabel = document.getElementById("qr_timer_message");
    const EXPIRY_MILLIS = 180000; // 3 minutes
    const expiryTime = Date.now() + EXPIRY_MILLIS;

    // clear any existing timer
    if (qrTimerInterval !== null) {
        clearInterval(qrTimerInterval);
    }

    qrTimerInterval = setInterval(function () {
        const remaining = expiryTime - Date.now();
        if (remaining <= 0) {
            clearInterval(qrTimerInterval);
            qrTimerInterval = null;
            qrTimerLabel.innerText = "QR Expired. Please generate a new QR code.";
            return;
        }
        const seconds = Math.floor(remaining / 1000);
        const mm = String(Math.floor(seconds / 60)).padStart(2, '0');
        const ss = String(seconds % 60).padStart(2, '0');
        qrTimerLabel.innerText = "QR expires in " + mm + ":" + ss;
    }, 1000);
}

function getHistoryTransactions() {
    var requestOptions = {
        method: 'GET',
        redirect: 'follow'
    };

    // fetch completed transactions from new history endpoint
    fetch("/transactions/history", requestOptions)
        .then(response => response.json())
        .then(result => createHistoryTable(result))
        .catch(error => console.log('error', error));
}

function createHistoryTable(transactions) {
    console.log(transactions);
    const tableBody = document.getElementById("history_body_table");
    resetTbody(tableBody);

    transactions.map(transaction => {
        var tr = document.createElement('tr');
        tr.innerHTML = `<tr>
                            <td>${transaction.transactionId}</td>
                            <td>${transaction.merchantName}</td>
                            <td>${transaction.amount} ${currencyEnum["INR"]}</td>
                            <td>${transaction.status}</td>
                            <td>${new Date(transaction.createdTimestamp).toLocaleString()}</td>
                        </tr>`;
        tableBody.appendChild(tr);
    })
}

function clearHistory() {
    if (!confirm("Are you sure you want to clear all completed transaction history?")) {
        return;
    }
    fetch("/transactions/history", {
        method: "DELETE"
    })
        .then(function (res) { return res.text(); })
        .then(function () {
            getHistoryTransactions();
        })
        .catch(function (err) {
            console.log("Failed to clear history", err);
        });
}

function getPendingTransactions() {
    var requestOptions = {
        method: 'GET',
        redirect: 'follow'
    };

    fetch("/transactions/pending", requestOptions)
        .then(response => response.json())
        .then(result => createPendingTable(result))
        .catch(error => console.log('error', error));
}

function createPendingTable(transactions) {
    const tableBody = document.getElementById("pending_body_table");
    resetTbody(tableBody);

    transactions.map(transaction => {
        var tr = document.createElement('tr');
        tr.innerHTML = `<tr>
                            <td>${transaction.transactionId}</td>
                            <td>${transaction.merchantName}</td>
                            <td>${transaction.amount} ${currencyEnum[userObj.currency] || ''}</td>
                            <td>${transaction.status}</td>
                            <td><a class="waves-effect waves-light btn" onclick="confirmTransaction('${transaction.transactionId}')">Confirm Payment</a></td>
                        </tr>`;
        tableBody.appendChild(tr);
    })
}

function confirmTransaction(transactionId) {
    var myHeaders = new Headers();
    myHeaders.append("Content-Type", "application/json");

    var raw = JSON.stringify({ "transactionId": transactionId });

    var requestOptions = {
        method: 'POST',
        headers: myHeaders,
        body: raw,
        redirect: 'follow'
    };

    fetch("/confirmPayment", requestOptions)
        .then(response => response.text())
        .then(result => {
            alert(result);
            // refresh lists after confirmation attempt
            getPendingTransactions();
            getHistoryTransactions();
        })
        .catch(error => console.log('error', error));
}

function resetTbody(tbody){
    for(var i = tbody.rows.length; i>0 ;i--)
        tbody.deleteRow(i-1);
}

// ===== Invoice-from-catalog inside web app =====

function loadCatalogForBill() {
    fetch("/items")
        .then(response => response.json())
        .then(items => {
            catalogItemsForBill = items;
            renderBillRows();
        })
        .catch(error => console.log('error', error));
}

function renderBillRows() {
    const tableBody = document.getElementById("bill_items_table");
    if (!tableBody) return;
    resetTbody(tableBody);

    catalogItemsForBill.forEach(item => {
        const tr = document.createElement("tr");
        tr.innerHTML = `<tr>
                            <td>${item.name}</td>
                            <td>${item.price.toFixed(2)}</td>
                            <td>
                                <input id="bill_price_${item.id}" type="number" min="0" step="0.01" value="${item.price.toFixed(2)}" onchange="updateBillTotal()">
                            </td>
                            <td>
                                <button class="btn-small" onclick="changeBillQty('${item.id}', -1)">-</button>
                                <span id="bill_qty_${item.id}" style="margin:0 10px;">0</span>
                                <button class="btn-small" onclick="changeBillQty('${item.id}', 1)">+</button>
                            </td>
                            <td><span id="bill_line_${item.id}">0.00</span></td>
                        </tr>`;
        tableBody.appendChild(tr);
    });
}

function changeBillQty(itemId, delta) {
    const span = document.getElementById("bill_qty_" + itemId);
    if (!span) return;
    let current = parseInt(span.innerText || "0", 10);
    current += delta;
    if (current < 0) current = 0;
    span.innerText = String(current);
    updateBillTotal();
}

function updateBillTotal() {
    let total = 0;
    catalogItemsForBill.forEach(item => {
        const qtySpan = document.getElementById("bill_qty_" + item.id);
        const priceInput = document.getElementById("bill_price_" + item.id);
        const lineSpan = document.getElementById("bill_line_" + item.id);
        if (!qtySpan || !priceInput || !lineSpan) return;

        const qty = parseInt(qtySpan.innerText || "0", 10);
        const price = parseFloat(priceInput.value || "0");
        const lineTotal = qty > 0 ? (price * qty) : 0;
        lineSpan.innerText = lineTotal.toFixed(2);
        total += lineTotal;
    });
    const totalInput = document.getElementById("bill_total");
    if (totalInput) {
        totalInput.value = total.toFixed(2);
    }
    const mobile = document.getElementById("customer_mobile") ? document.getElementById("customer_mobile").value : "";
    const customerInfo = mobile ? ("Bill will be sent to: " + mobile) : "";
    if (document.getElementById("bill_customer_info")) {
        document.getElementById("bill_customer_info").innerText = customerInfo;
    }
}

function generateInvoiceFromApp() {
    const total = document.getElementById("bill_total").value;
    const mobile = document.getElementById("customer_mobile").value;

    if (!total || parseFloat(total) <= 0) {
        alert("Please select at least one item with a valid price.");
        return;
    }
    if (!mobile) {
        alert("Please enter customer mobile number.");
        return;
    }

    let items = [];
    catalogItemsForBill.forEach(item => {
        const qtySpan = document.getElementById("bill_qty_" + item.id);
        const priceInput = document.getElementById("bill_price_" + item.id);
        if (!qtySpan || !priceInput) return;
        const qty = parseInt(qtySpan.innerText || "0", 10);
        const price = parseFloat(priceInput.value || "0");
        if (qty > 0 && price > 0) {
            items.push({
                itemId: item.id,
                quantity: qty,
                price: price
            });
        }
    });

    if (items.length === 0) {
        alert("Please select at least one item with quantity > 0.");
        return;
    }

    const payload = {
        merchantName: userObj.name,
        upiId: userObj.upiId,
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
            // redirect to invoice page inside same app
            window.location.href = "/invoice/" + transactionId;
        })
        .catch(error => console.log('error', error));
}

// ===== Dashboard inside main interface =====
function showDashboard() {
    var dateInput = document.getElementById("dashboard_date");
    var dateValue = dateInput ? dateInput.value : "";
    var url = "/dashboard-stats";
    if (dateValue) {
        url += "?date=" + encodeURIComponent(dateValue);
    }
    fetch(url)
        .then(function (res) { return res.json(); })
        .then(function (data) {
            document.getElementById("txCount").innerText = data.transactions;
            document.getElementById("revenue").innerText = data.totalRevenue;
            document.getElementById("pending").innerText = data.pendingPayments;
        })
        .catch(function (err) {
            console.log("Failed to load dashboard stats", err);
        });
}