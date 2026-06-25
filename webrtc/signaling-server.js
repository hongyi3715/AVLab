const WebSocket = require('ws');
const wss = new WebSocket.Server({ port: 8080 });
const rooms = {};

wss.on('connection', (ws) => {
    // 🌟 只要有新手机连进来，立刻打印！
    console.log(`[系统提示] 收到一个新的网络连接！`);

    ws.on('message', (data) => {
        let msg;
        try {
            msg = JSON.parse(data.toString());
        } catch(e) {
            console.log(`[收到未知文本]: ${data.toString()}`);
            return;
        }

        // 🌟 收到任何类型的消息（join, offer, answer, candidate）都打印出来！
        console.log(`[收到消息] 房间: ${msg.room || ws.room || '未知'}, 类型: ${msg.type}`);

        if (msg.type === 'join') {
            ws.room = msg.room;
            rooms[msg.room] = rooms[msg.room] || [];
            rooms[msg.room].push(ws);
            console.log(`[房间动态] 客户端加入房间 ${msg.room}, 当前房间总人数: ${rooms[msg.room].length}`);
            return;
        }

        // 转发给同房间内的其他连接
        const peers = rooms[ws.room] || [];
        peers.forEach(client => {
            if (client !== ws && client.readyState === WebSocket.OPEN) {
                client.send(data.toString());
            }
        });
    });

    ws.on('close', () => {
        console.log(`[系统提示] 一个连接断开了...`);
        if (ws.room && rooms[ws.room]) {
            rooms[ws.room] = rooms[ws.room].filter(c => c !== ws);
            console.log(`[房间动态] 房间 ${ws.room} 剩余人数: ${rooms[ws.room].length}`);
        }
    });
});

console.log('Signaling server running on ws://0.0.0.0:8080');