const discord = require('discord.js');
const config = require("./config.json");
const jsonfile = require('jsonfile');
const claims = require('./claims.json');
let blacklist = require('./blacklist.json');
const commands = {};
let client = null;

commands.writeConfig = () => jsonfile.writeFile('config.json', config, {spaces: 4}, err => {
    if (err) console.error(err);
});

commands.writeClaims = () => jsonfile.writeFile('claims.json', claims, {spaces: 4}, err => {
    if (err) console.error(err);
});

commands.writeBlacklist = () => jsonfile.writeFile('blacklist.json', blacklist, {spaces: 4}, err => {
    if (err) console.error(err);
});

commands.getRole = identifier => {
    if (typeof identifier === 'string')
        if (identifier.match(/\d+/g))
            identifier = identifier.match(/\d+/g);
    for (let guild of client.guilds.values())
        for (let role of guild.roles.values())
            if (role.id == identifier || role.name == identifier)
                return role;
    return null;
};

commands.getChannel = identifier => {
    if (typeof identifier === 'string')
        if (identifier.match(/\d+/g))
            identifier = identifier.match(/\d+/g);
    for (let guild of client.guilds.values())
        for (let channel of guild.channels.values())
            if (channel.id == identifier || channel.name == identifier)
                return channel;
    return null;
};

commands.getUser = (guild, identifier) => {
    if (typeof identifier === 'string')
        if (identifier.match(/\d+/g))
            identifier = identifier.match(/\d+/g);
    for (let member of guild.members.values())
        if (member.id == identifier || member.name == identifier)
            return member;
    return null;
};

commands.getClaims = user => {
    if (claims[user.id] === undefined)
        return claims[user.id] = {};
    else
        return claims[user.id];
}

commands.createPaginator = async (sourceMessage, message, next, prev) => {
    const emojinext = "▶";
    const emojiprev = "◀";
    const emojistop = "❌";
    try {
        await message.react(emojiprev);
        await message.react(emojinext);
        await message.react(emojistop);
        message.createReactionCollector((reaction, user) =>
            user.id === sourceMessage.author.id && (
                reaction.emoji.name === emojinext ||
                reaction.emoji.name === emojiprev ||
                reaction.emoji.name === emojistop
            )).on('collect', reaction => {
            switch (reaction.emoji.name) {
                case emojinext:
                    next();
                    break;
                case emojiprev:
                    prev();
                    break;
                case emojistop:
                    message.delete().catch(e => console.log(e));
                    sourceMessage.delete().catch(e => console.log(e));
                    break;
                default:
                    console.log('Something went processing emoji reactions.');
                    break;
            }
        });
    } catch (error) {
        console.log('Error involving reaction collector.');
    }
};

commands.onMessage = async message => {
    if (message.author.bot)
        return;
    if (message.content.indexOf(config.prefix) !== 0)
        return;
    if (!message.member.hasPermission("MANAGE_ROLES")
        && !(client.user.id == 431980306111660062 && message.author.id == 159018622600216577))
        return message.channel.send("You do not have permissions to use the bot.");
    let args = message.content.slice(config.prefix.length).trim().split(/\s+/g);
    let command = args.shift().toLocaleLowerCase();
    for (let cmd of commands.list)
        if (cmd.name === command)
            return cmd.action(message, args);
    message.channel.send(`No command found matching '${command}'`);
};

commands.init = cl => {
    client = cl;
    client.on('message', message => commands.onMessage(message));
    return commands;
};
module.exports = commands;
commands.list = [];

function addCommand(adminonly, name, action) {
    commands.list.push({name: name, action: action, adminonly: adminonly});
}

addCommand(false, "help", async (message, args) => {
    message.channel.send(new discord.RichEmbed()
        .setTitle("Commands")
        .setDescription(commands.list.map(cmd => cmd.name).join('\n')));
});

addCommand(false, "inforaw", async (message, args) => {
    let embed = new discord.RichEmbed()
        .setTitle("config.json")
        .setColor("GRAY");
    for (let configKey in config) {
        embed.addField(configKey, config[configKey]);
    }
    message.channel.send(embed);
});

addCommand(true, "eval", async (message, args) => {
    try {
        message.channel.send(`>${eval(args.join(" "))}`);
    } catch (error) {
        message.channel.send(`Error:${error}`);
    }
});

addCommand(true, "setraw", async (message, args) => {
    try {
        config[args.shift()] = eval(args.join(" "));
        commands.writeConfig();
    } catch (error) {
        message.channel.send(`Error: ${error}`);
    }
});

addCommand(false, "claim", async (message, args) => {
    let role = commands.getRole(args.join(" "));
    if (role !== null) {
        if (blacklist.includes(role.id)) {
            message.channel.send(new discord.RichEmbed().setColor("ORANGE").setDescription(`You are not allowed to claim ${role}.`));
        } else if (!message.member.roles.has(role.id)) {
            message.channel.send(new discord.RichEmbed().setColor("ORANGE").setDescription(`You do not have the ${role} role.`));
        } else {
            commands.getClaims(message.author)[role.id] = {
                name: config.max_changes_name,
                colour: config.max_changes_colour
            };
            commands.writeClaims();
            message.channel.send(new discord.RichEmbed().setColor("GREEN").setDescription(`You have claimed ${role}.`));
        }
    } else {
        message.channel.send("No role found with that id.");
    }
});

addCommand(false, "mine", async (message, args) => {
    let claims = commands.getClaims(message.author);
    let embed = new discord.RichEmbed()
        .setTitle(`${message.author.username} Claims Info`)
        .setColor("CYAN")
        .setThumbnail(message.author.avatarURL)
        .setDescription("");
    for (let id of Object.keys(claims))
        embed.description += (`<@&${id}> ${claims[id].name} ${claims[id].colour}`);
    message.channel.send(embed);
});

addCommand(true, "blacklist", async (message, args) => {
    let role = commands.getRole(args.join(" "));
    if (role !== null) {
        if (blacklist.includes(role.id)) {
            message.channel.send(new discord.RichEmbed().setColor("ORANGE").setDescription(`${role} is already in the blacklist.`));
        } else {
            blacklist.push(role.id);
            commands.writeBlacklist();
            message.channel.send(new discord.RichEmbed().setColor("GREEN").setDescription(`${role} has been added to the blacklist.`));
        }

    } else {
        message.channel.send("No role found with that id.");
    }
});

addCommand(true, "unblacklist", async (message, args) => {
    let role = commands.getRole(args.join(" "));
    if (role !== null) {
        for (let i in blacklist) {
            if (blacklist[i] === role.id) {
                blacklist.splice(i, 1);
                commands.writeBlacklist();
                message.channel.send(new discord.RichEmbed().setColor("GREEN").setDescription(`${role} has been removed from the blacklist.`));
                return;
            }
        }
        message.channel.send(new discord.RichEmbed().setColor("ORANGE").setDescription(`${role} was not in the blacklist.`));
    } else {
        message.channel.send("No role found with that id.");
    }
});

addCommand(true, "pregenblacklist", async (message, args) => {
    let start = blacklist.length;
    for (let role of message.guild.roles.values())
        if (!blacklist.includes(role.id))
            if (role.members.size > 1)
                blacklist.push(role.id);
    commands.writeBlacklist();
    message.channel.send(new discord.RichEmbed().setColor("GREEN").setDescription(`Added ${blacklist.length - start} values to the blacklist.`));
});

// addCommand(false, "listRoles")