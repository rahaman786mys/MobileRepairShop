/**
 * MuZZu Tech — WhatsApp Cloud API Cloud Functions
 *
 * Prerequisites:
 * 1. Meta Business Account: https://business.facebook.com/
 * 2. WhatsApp App: https://developers.facebook.com/apps/
 * 3. Permanent Access Token + Phone Number ID
 *
 * After deploying, set these config values:
 *   firebase functions:config:set whatsapp.phone_number_id="YOUR_PHONE_NUMBER_ID"
 *   firebase functions:config:set whatsapp.access_token="YOUR_PERMANENT_ACCESS_TOKEN"
 *   firebase functions:config:set whatsapp.api_version="v18.0"
 */

const { onCall, onRequest } = require("firebase-functions/v2/https");
const { defineString } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const axios = require("axios");

initializeApp();

// -------------------------------------------------------------------
// Configuration (set via firebase functions:config:set)
// -------------------------------------------------------------------
const WHATSAPP_API_VERSION = "v18.0";
const WHATSAPP_BASE_URL = `https://graph.facebook.com/${WHATSAPP_API_VERSION}`;

// -------------------------------------------------------------------
// Helper: Send WhatsApp message via Cloud API
// -------------------------------------------------------------------
async function sendWhatsAppMessage(phoneNumberId, accessToken, to, messageData) {
  const url = `${WHATSAPP_BASE_URL}/${phoneNumberId}/messages`;
  const payload = {
    messaging_product: "whatsapp",
    recipient_type: "individual",
    to: to.replace(/[^0-9]/g, ""),
    ...messageData,
  };

  const response = await axios.post(url, payload, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
  });
  return response.data;
}

// -------------------------------------------------------------------
// sendWhatsApp — Callable function (called from mobile app)
// -------------------------------------------------------------------
exports.sendWhatsApp = onCall(async (request) => {
  const auth = request.auth;
  if (!auth) {
    throw new Error("Authentication required");
  }

  const { phone, message, type } = request.data;
  if (!phone || !message) {
    throw new Error("Phone and message are required");
  }

  const phoneNumberId = process.env.WHATSAPP_PHONE_NUMBER_ID;
  const accessToken = process.env.WHATSAPP_ACCESS_TOKEN;

  if (!phoneNumberId || !accessToken) {
    throw new Error("WhatsApp API not configured. Set WHATSAPP_PHONE_NUMBER_ID and WHATSAPP_ACCESS_TOKEN");
  }

  try {
    const messageData = { text: { body: message } };
    const result = await sendWhatsAppMessage(phoneNumberId, accessToken, phone, {
      type: "text",
      text: { body: message },
    });

    // Log to Firestore
    await getFirestore().collection("whatsapp_logs").add({
      phone,
      message: message.substring(0, 200),
      type: type || "text",
      status: "sent",
      messageId: result.messages?.[0]?.id || "",
      sentAt: new Date().toISOString(),
      sentBy: auth.uid,
    });

    return { success: true, messageId: result.messages?.[0]?.id };
  } catch (error) {
    console.error("WhatsApp send error:", error.response?.data || error.message);
    throw new Error(error.response?.data?.error?.message || "Failed to send WhatsApp message");
  }
});

// -------------------------------------------------------------------
// sendTemplate — Send a pre-approved WhatsApp template
// -------------------------------------------------------------------
exports.sendTemplate = onCall(async (request) => {
  const auth = request.auth;
  if (!auth) throw new Error("Authentication required");

  const { phone, templateName, languageCode, components } = request.data;
  if (!phone || !templateName) {
    throw new Error("Phone and templateName are required");
  }

  const phoneNumberId = process.env.WHATSAPP_PHONE_NUMBER_ID;
  const accessToken = process.env.WHATSAPP_ACCESS_TOKEN;

  try {
    const result = await sendWhatsAppMessage(phoneNumberId, accessToken, phone, {
      type: "template",
      template: {
        name: templateName,
        language: { code: languageCode || "en" },
        components: components || [],
      },
    });

    await getFirestore().collection("whatsapp_logs").add({
      phone,
      templateName,
      status: "sent",
      messageId: result.messages?.[0]?.id || "",
      sentAt: new Date().toISOString(),
      sentBy: auth.uid,
    });

    return { success: true, messageId: result.messages?.[0]?.id };
  } catch (error) {
    console.error("Template send error:", error.response?.data || error.message);
    throw new Error(error.response?.data?.error?.message || "Failed to send template");
  }
});

// -------------------------------------------------------------------
// sendOtp — Send OTP via WhatsApp (uses template)
// -------------------------------------------------------------------
exports.sendOtp = onCall(async (request) => {
  const { phone, otp } = request.data;
  if (!phone || !otp) throw new Error("Phone and OTP are required");

  const phoneNumberId = process.env.WHATSAPP_PHONE_NUMBER_ID;
  const accessToken = process.env.WHATSAPP_ACCESS_TOKEN;
  const otpTemplate = process.env.WHATSAPP_OTP_TEMPLATE || "otp_template";

  try {
    const result = await sendWhatsAppMessage(phoneNumberId, accessToken, phone, {
      type: "template",
      template: {
        name: otpTemplate,
        language: { code: "en" },
        components: [
          {
            type: "body",
            parameters: [{ type: "text", text: otp }],
          },
        ],
      },
    });

    // Store OTP in Firestore for verification
    await getFirestore().collection("otp_codes").doc(phone).set({
      otp,
      phone,
      verified: false,
      expiresAt: new Date(Date.now() + 5 * 60 * 1000).toISOString(),
      createdAt: new Date().toISOString(),
    });

    return { success: true, messageId: result.messages?.[0]?.id };
  } catch (error) {
    console.error("OTP send error:", error.response?.data || error.message);
    throw new Error("Failed to send OTP");
  }
});

// -------------------------------------------------------------------
// verifyOtp — Verify OTP code
// -------------------------------------------------------------------
exports.verifyOtp = onCall(async (request) => {
  const { phone, otp } = request.data;
  if (!phone || !otp) throw new Error("Phone and OTP are required");

  const doc = await getFirestore().collection("otp_codes").doc(phone).get();
  if (!doc.exists) throw new Error("No OTP sent to this number");

  const data = doc.data();
  if (data.verified) throw new Error("OTP already used");
  if (new Date(data.expiresAt) < new Date()) throw new Error("OTP expired");

  if (data.otp !== otp) throw new Error("Invalid OTP");

  await doc.ref.update({ verified: true, verifiedAt: new Date().toISOString() });
  return { success: true };
});

// -------------------------------------------------------------------
// Webhook — For receiving incoming messages (Meta verification)
// -------------------------------------------------------------------
exports.whatsappWebhook = onRequest(async (req, res) => {
  // Meta webhook verification (GET)
  if (req.method === "GET") {
    const mode = req.query["hub.mode"];
    const token = req.query["hub.verify_token"];
    const challenge = req.query["hub.challenge"];
    const verifyToken = process.env.WHATSAPP_WEBHOOK_VERIFY_TOKEN || "muzzutech_verify_2024";

    if (mode === "subscribe" && token === verifyToken) {
      console.log("Webhook verified successfully");
      res.status(200).send(challenge);
    } else {
      res.status(403).send("Verification failed");
    }
    return;
  }

  // Incoming messages (POST)
  if (req.method === "POST") {
    const entry = req.body?.entry?.[0];
    const change = entry?.changes?.[0];
    const value = change?.value;
    const messages = value?.messages;

    if (messages) {
      for (const msg of messages) {
        const from = msg.from; // sender phone
        const text = msg.text?.body || "";
        const timestamp = msg.timestamp;

        // Store incoming message in Firestore
        await getFirestore().collection("incoming_messages").add({
          from,
          message: text,
          messageId: msg.id,
          timestamp: new Date(parseInt(timestamp) * 1000).toISOString(),
          receivedAt: new Date().toISOString(),
        });

        // Auto-reply for common keywords
        const reply = getAutoReply(text.toLowerCase());
        if (reply) {
          const phoneNumberId = process.env.WHATSAPP_PHONE_NUMBER_ID;
          const accessToken = process.env.WHATSAPP_ACCESS_TOKEN;
          if (phoneNumberId && accessToken) {
            try {
              await sendWhatsAppMessage(phoneNumberId, accessToken, from, {
                type: "text",
                text: { body: reply },
              });
            } catch (e) {
              console.error("Auto-reply failed:", e.message);
            }
          }
        }
      }
    }

    res.sendStatus(200);
  }
});

// -------------------------------------------------------------------
// Auto-reply helper
// -------------------------------------------------------------------
function getAutoReply(text) {
  if (text.includes("hello") || text.includes("hi") || text.includes("hey")) {
    return "Hello! Welcome to MuZZu Tech. How can we help you today?\n\n1️⃣ New Repair\n2️⃣ Check Status\n3️⃣ Contact Shop";
  }
  if (text.includes("1") || text.includes("repair")) {
    return "To start a new repair, please visit our shop with your device. Our technicians will inspect and provide a quote.";
  }
  if (text.includes("2") || text.includes("status")) {
    return "Please share your repair ticket number to check the status.";
  }
  if (text.includes("3") || text.includes("contact") || text.includes("shop")) {
    return "📍 Visit us at our shop or call us for assistance.\nWe're happy to help!";
  }
  return null;
}
