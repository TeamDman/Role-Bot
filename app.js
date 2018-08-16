'use strict';
const config = require('./config');
const discord = require('discord.js');
const token = require('./token.json').token;
const client = new discord.Client();
let commands;

client.on('ready', () => {
    console.log(`Bot has started, with ${client.users.size} users, in ${client.channels.size} channels of ${client.guilds.size} guilds.`);
    client.user.setActivity(config.presence, {type:config.presence_type});
    commands = require("./commands.js").init(client);
});

console.log("App is starting");
client.login(token);
