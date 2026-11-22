const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const SHOP_OWNER_UID = "shop_owner_ahmad_123";
const OTHER_USER_UID = "other_user_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProductPayload(now = new Date(Date.now() - 2000)) {
  return {
    nameUrdu: "گندم کا آٹا 20 کلو",
    nameEnglish: "Wheat Flour 20kg Bag",
    category: "FLOUR_RICE",
    unit: "bag",
    costPrice: 2750,
    sellingPrice: 2900,
    marginPercent: 5.5,
    autoPriceEnabled: true,
    stockQty: 25,
    lowStockThreshold: 5,
    barcode: "8961001000011",
    createdAt: now,
    updatedAt: now,
  };
}

function validCustomerPayload(now = new Date(Date.now() - 2000)) {
  return {
    name: "حاجی محمد اسلم",
    phone: "0300-1234567",
    address: "محلہ سرکلر روڈ، گجرات",
    balance: 1500,
    createdAt: now,
    updatedAt: now,
  };
}

function validOrderPayload(status = "PENDING", now = new Date(Date.now() - 2000)) {
  return {
    billNumber: "AB-1001",
    customerId: "cust_1",
    customerName: "حاجی محمد اسلم",
    status,
    paymentMethod: "UDHAR",
    subtotal: 2900,
    discount: 0,
    totalAmount: 2900,
    paidAmount: 0,
    remainingUdhar: 2900,
    totalCost: 2750,
    profitAmount: 150,
    items: [
      {
        productId: "prod_atta_20kg",
        nameUrdu: "گندم کا آٹا 20 کلو",
        nameEnglish: "Wheat Flour 20kg Bag",
        unit: "bag",
        quantity: 1,
        unitPrice: 2900,
        costPrice: 2750,
        lineTotal: 2900,
      },
    ],
    note: "شام کو ڈلیوری",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated users cannot read or write any shop collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${SHOP_OWNER_UID}/products`).get());
  await assertFails(unauthDb.collection(`users/${SHOP_OWNER_UID}/customers`).get());
  await assertFails(unauthDb.collection(`users/${SHOP_OWNER_UID}/orders`).get());
  await assertFails(unauthDb.collection(`users/${SHOP_OWNER_UID}/ledgerEntries`).get());
  await assertFails(unauthDb.collection(`users/${SHOP_OWNER_UID}/stockMovements`).get());
  await assertFails(
    unauthDb
      .collection(`users/${SHOP_OWNER_UID}/products`)
      .doc("prod_1")
      .set(validProductPayload())
  );
});

test("Cross-user isolation: another authenticated user cannot read or write owner's shop data", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection(`users/${SHOP_OWNER_UID}/products`)
      .doc("prod_1")
      .set(validProductPayload());
    await ctx
      .firestore()
      .collection(`users/${SHOP_OWNER_UID}/customers`)
      .doc("cust_1")
      .set(validCustomerPayload());
  });

  const otherDb = testEnv.authenticatedContext(OTHER_USER_UID).firestore();
  await assertFails(otherDb.collection(`users/${SHOP_OWNER_UID}/products`).get());
  await assertFails(
    otherDb.collection(`users/${SHOP_OWNER_UID}/products`).doc("prod_1").get()
  );
  await assertFails(
    otherDb
      .collection(`users/${SHOP_OWNER_UID}/products`)
      .doc("prod_1")
      .update({ sellingPrice: 9999, updatedAt: new Date(Date.now() - 1000) })
  );
  await assertFails(
    otherDb.collection(`users/${SHOP_OWNER_UID}/customers`).doc("cust_1").get()
  );
});

test("Products: owner can create, list, update price/stock, and delete; invalid payloads rejected", async () => {
  const ownerDb = testEnv.authenticatedContext(SHOP_OWNER_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/products`)
      .doc("prod_atta_20kg")
      .set(validProductPayload(created))
  );

  await assertSucceeds(
    ownerDb.collection(`users/${SHOP_OWNER_UID}/products`).get()
  );

  // Stock deduction partial update
  await assertSucceeds(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/products`)
      .doc("prod_atta_20kg")
      .update({
        stockQty: 23,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Ghost field rejected (Shadow Update test)
  await assertFails(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/products`)
      .doc("prod_ghost")
      .set({ ...validProductPayload(created), id: "prod_ghost" })
  );

  // Immutable createdAt mutation rejected
  await assertFails(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/products`)
      .doc("prod_atta_20kg")
      .update({
        createdAt: new Date(Date.now() - 500),
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Future timestamp rejected
  await assertFails(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/products`)
      .doc("prod_future")
      .set(validProductPayload(new Date(Date.now() + 60000)))
  );

  // Invalid category rejected
  await assertFails(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/products`)
      .doc("prod_bad_cat")
      .set({ ...validProductPayload(created), category: "INVALID_CAT" })
  );

  await assertSucceeds(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/products`)
      .doc("prod_atta_20kg")
      .delete()
  );
});

test("Customers & Udhar Khata ledgerEntries: create, balance update, append-only ledger", async () => {
  const ownerDb = testEnv.authenticatedContext(SHOP_OWNER_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/customers`)
      .doc("cust_1")
      .set(validCustomerPayload(created))
  );

  await assertSucceeds(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/customers`)
      .doc("cust_1")
      .update({
        balance: 500,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  await assertSucceeds(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/ledgerEntries`)
      .doc("led_1")
      .set({
        customerId: "cust_1",
        customerName: "حاجی محمد اسلم",
        entryType: "CREDIT",
        amount: 1000,
        balanceAfter: 500,
        orderId: "",
        note: "نقد وصولی",
        createdAt: created,
      })
  );

  // Ledger entries are append-only (no update)
  await assertFails(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/ledgerEntries`)
      .doc("led_1")
      .update({ amount: 5000 })
  );
});

test("Orders: pending orders can transition to COMPLETED; completed orders are terminally locked", async () => {
  const ownerDb = testEnv.authenticatedContext(SHOP_OWNER_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/orders`)
      .doc("ord_1")
      .set(validOrderPayload("PENDING", created))
  );

  // Transition PENDING -> COMPLETED succeeds
  await assertSucceeds(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/orders`)
      .doc("ord_1")
      .update({
        status: "COMPLETED",
        paymentMethod: "CASH",
        paidAmount: 2900,
        remainingUdhar: 0,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Terminal state lock: COMPLETED order cannot be mutated
  await assertFails(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/orders`)
      .doc("ord_1")
      .update({
        totalAmount: 100,
        updatedAt: new Date(Date.now() - 500),
      })
  );
});

test("StockMovements: owner can log Stock In/Out/Sale; append-only enforced", async () => {
  const ownerDb = testEnv.authenticatedContext(SHOP_OWNER_UID).firestore();
  const created = new Date(Date.now() - 4000);

  await assertSucceeds(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/stockMovements`)
      .doc("mov_1")
      .set({
        productId: "prod_atta_20kg",
        productNameUrdu: "گندم کا آٹا 20 کلو",
        productNameEnglish: "Wheat Flour 20kg Bag",
        movementType: "STOCK_IN",
        quantity: 10,
        unitCost: 2750,
        unitPrice: 2900,
        profitDelta: 0,
        note: "گجرات منڈی سے خریداری",
        createdAt: created,
      })
  );

  await assertFails(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/stockMovements`)
      .doc("mov_1")
      .update({ quantity: 50 })
  );
});

test("RationDonations: owner can record donations & distributions; append-only & isolated", async () => {
  const ownerDb = testEnv.authenticatedContext(SHOP_OWNER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_USER_UID).firestore();
  const created = new Date(Date.now() - 4000);

  await assertSucceeds(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/donations`)
      .doc("don_1")
      .set({
        entryType: "DONATION_IN",
        personName: "مخیر حضرات",
        phone: "0300-1234567",
        amount: 6500,
        rationSummary: "ایک مستحق خاندان کے لیے راشن",
        paymentRef: "HBL-16857900668003",
        createdAt: created,
      })
  );

  // Append-only enforced
  await assertFails(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/donations`)
      .doc("don_1")
      .update({ amount: 100 })
  );

  // Cross-user read denied
  await assertFails(
    otherDb.collection(`users/${SHOP_OWNER_UID}/donations`).get()
  );
});

test("Maryam Nawaz Ration Card items & NFC bills: CRUD on items, append-only bills, strict isolation", async () => {
  const ownerDb = testEnv.authenticatedContext(SHOP_OWNER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_USER_UID).firestore();
  const created = new Date(Date.now() - 4000);

  await assertSucceeds(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/rationItems`)
      .doc("mn_atta_10kg")
      .set({
        nameUrdu: "گندم کا آٹا 10 کلو تھیلا",
        nameEnglish: "Wheat Flour 10kg Bag",
        unit: "bag",
        price: 880,
        sortOrder: 1,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Manual price edit succeeds
  await assertSucceeds(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/rationItems`)
      .doc("mn_atta_10kg")
      .update({
        price: 920,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Negative price rejected
  await assertFails(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/rationItems`)
      .doc("mn_atta_10kg")
      .update({
        price: -50,
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Valid NFC Ration Card bill succeeds
  await assertSucceeds(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/rationCardBills`)
      .doc("rcb_1")
      .set({
        billNumber: "MN-1001",
        cardUid: "04:A2:F1:8C:19:4E:80",
        cardNumber: "BOP-34201-8899",
        beneficiaryName: "محمد اکرم",
        cnic: "34201-1234567-1",
        phone: "0300-7654321",
        monthKey: "2026-11",
        subsidyLimit: 3000,
        totalAmount: 3000,
        subsidyUsed: 3000,
        cashTopUp: 0,
        remainingSubsidy: 0,
        items: [
          {
            productId: "mn_atta_10kg",
            nameUrdu: "گندم کا آٹا 10 کلو تھیلا",
            nameEnglish: "Wheat Flour 10kg Bag",
            unit: "bag",
            quantity: 2,
            unitPrice: 920,
            costPrice: 920,
            lineTotal: 1840,
          },
        ],
        scannedViaNfc: true,
        note: "ٹانڈہ راشن کارڈ",
        createdAt: created,
      })
  );

  // Append-only enforced on rationCardBills
  await assertFails(
    ownerDb
      .collection(`users/${SHOP_OWNER_UID}/rationCardBills`)
      .doc("rcb_1")
      .update({ subsidyUsed: 100 })
  );

  // Cross-user isolation enforced
  await assertFails(
    otherDb.collection(`users/${SHOP_OWNER_UID}/rationCardBills`).get()
  );
  await assertFails(
    otherDb.collection(`users/${SHOP_OWNER_UID}/rationItems`).get()
  );
});
