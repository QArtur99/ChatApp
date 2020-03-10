const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);
const db = admin.firestore();

exports.sendNotification = functions.firestore
    .document('chatRooms/{chatRoomId}/chatRoom/{messageId}')
    .onCreate((snap, context) => {
        const data = snap.data();
        sendNotification(data);
        return null;
    });

async function sendNotification(data) {
    try {
        const user = await db.collection("users").doc(data.receiverId).get()
            .then((result) => {
                console.log('Successfully got user:', result);
                return result;
            })
            .catch((error) => {
                console.log('Error getting user:', error);
            });

        user.data().fcmToken.map((fcmToken) => {
            const message = createMessage(data, fcmToken);
            admin.messaging().send(message)
                .then((messageIdString) => {
                    console.log('Successfully sent message:', messageIdString);
                    return messageIdString;
                })
                .catch((error) => {
                    console.log('Error sending message:', error);
                });
        });
    } catch (e) {
        console.log('Error:', e);
    }
    return null;
}

function createMessage(data, fcmToken) {
    return message = {
        data: {
            id: getStringValue(data.id),
            senderId: getStringValue(data.senderId),
            receiverId: getStringValue(data.receiverId),
            isOwner: getStringValue(data.isOwner),
            name: getStringValue(data.name),
            photoUrl: getStringValue(data.photoUrl),
            audioUrl: getStringValue(data.audioUrl),
            audioFile: getStringValue(data.audioFile),
            audioDuration: getStringValue(data.audioDuration),
            text: getStringValue(data.text),
            timestamp: getStringValue(data.timestamp),
            readTimestamp: getStringValue(data.readTimestamp),
        },
        token: fcmToken
    };
}

function getStringValue(data) {
    if (typeof data === 'undefined' || data === null) return "";
    return data.toString();
}

function getTimestampValue(data) {
    if (typeof data === 'undefined' || data === null) return "";
    return data.seconds.toString();
}
