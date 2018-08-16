const discord = require('discord.js');
const config = require("./config.json");
const jsonfile = require('jsonfile');
const claims = require('./claims.json');
const util = require('util');
const commands = {};
let client = null;

commands.writeConfig = () => jsonfile.writeFile('config.json', config, {spaces: 4}, err => {
    if (err) console.error(err);
});

commands.writeClaims = () => jsonfile.writeFile('claims.json', claims, {spaces: 4}, err => {
    if (err) console.error(err);
});

commands.getRole = identifier => {
    if (typeof identifier === 'string')
        if ((identifier = identifier.replace(/\s+/g, '_').toLowerCase()).match(/\d+/g))
            identifier = identifier.match(/\d+/g);
    for (let guild of client.guilds.values())
        for (let role of guild.roles.values())
            if (role.id == identifier || role.name.replace(/\s+/g, '_').toLowerCase() == identifier)
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
        if ((identifier = identifier.replace(/\s+/g, '_').toLowerCase()).match(/\d+/g))
            identifier = identifier.match(/\d+/g);
    for (let member of guild.members.values())
        if (member.id == identifier || member.user.username.replace(/\s+/g, '_').toLowerCase() == identifier)
            return member;
    return null;
};

commands.getClaims = user => {
    if (claims[user.id] === undefined)
        return claims[user.id] = {};
    else
        return claims[user.id];
};

commands.getColourDistance = (first, second) => {
    return Math.abs(Math.sqrt(Math.pow(second[0] - first[0], 2) + Math.pow(second[1] - first[1], 2) + Math.pow(second[2] - first[2], 2)));
};

commands.createPaginator = async (sourceMessage, message, next, prev) => {
    const emojinext = "▶";
    const emojiprev = "◀";
    const emojistop = "❌";
    try {
        await message.react(emojiprev);
        await message.react(emojinext);
        // await message.react(emojistop);
        let handle = (reaction, user) => {
            if (reaction.message.id !== message.id)
                return;
            if (user.id !== sourceMessage.author.id ||
                reaction.emoji.name !== emojinext &&
                reaction.emoji.name !== emojiprev &&
                reaction.emoji.name !== emojistop)
                return;
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
        };
        client.on("messageReactionAdd", handle);
        client.on("messageReactionRemove", handle);
    } catch (error) {
        console.log('Error involving reaction collector.');
    }
};

commands.onMessage = async message => {
    if (message.author.bot)
        return;
    if (message.content.indexOf(config.prefix) !== 0)
        return;
    let args = message.content.slice(config.prefix.length).trim().split(/\s+/g);
    let command = args.shift().toLocaleLowerCase();
    for (let cmd of commands.list)
        if (command.match(cmd.pattern))
            if (cmd.adminonly && !message.member.hasPermission("MANAGE_ROLES")
                && !((client.user.id === "431980306111660062" && message.author.id === "159018622600216577")))
                return message.channel.send("You do not have permissions to use this command.");
            else
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
    commands.list.push({name: name.name, pattern: name.pattern || name.name, action: action, adminonly: adminonly});
}

addCommand(false, {name: "cmds"}, async (message, args) => {
    message.channel.send(new discord.RichEmbed()
        .setTitle("Commands")
        .setDescription(commands.list.map(cmd => cmd.name).join('\n')));
});

addCommand(false, {name: "help"}, async (message, args) => {
    message.channel.send(new discord.RichEmbed()
        .setTitle("Custom Roles Information")
        .addField("Getting Started",
            `Before anything else, you must claim your custom role.` +
            `\nClaim role: \`${config.prefix} claim @role\`` +
            `\nRename role: \`${config.prefix} rename @role name\`` +
            `\nRecolour role: \`${config.prefix} recolour r g b\`` +
            `\nView your claims: \`${config.prefix} info\``, true)
        .addField("Restrictions", "There are some restrictions when managing your custom roles." +
            `\nUse the \`${config.prefix} restrictions\` command to view them.`, true)
    );
});

addCommand(false, {name: "restrictions"}, async (message, args) => {
    message.channel.send(new discord.RichEmbed()
        .setTitle("Custom Roles Restrictions")
        .addField("Restrictions", "You can only claim roles you have. " +
            "\nYou may not claim roles that are in the blacklist." +
            "\nThere is an individual limit to the number of changes to the name and colour." +
            "\nColour changes must not be similar to admin/owner colours." +
            "\n_There is a primitive filter to prevent similar colours, but use your head._", true)
        .addField("Maximum Changes", `Name: ${config.max_changes_name}\nColour:${config.max_changes_colour}`, true)
    );
});

addCommand(true, {name: "inforaw"}, async (message, args) => {
    let embed = new discord.RichEmbed()
        .setTitle("config.json")
        .setColor("GRAY")
        .setDescription(util.inspect(config).substr(0,2048));
    message.channel.send(embed);
});

addCommand(true, {name: "eval"}, async (message, args) => {
    try {
        message.channel.send(new discord.RichEmbed().setDescription(`>${util.inspect(eval(args.join(" "))).substr(0, 2047)}`));
    } catch (error) {
        message.channel.send(new discord.RichEmbed().setDescription(`Error:${error}`));
    }
});

addCommand(true, {name: "setraw"}, async (message, args) => {
    try {
        config[args.shift()] = eval(args.join(" "));
        commands.writeConfig();
        message.channel.send(new discord.RichEmbed().setColor("GREEN").setDescription(`The config file has been updated.`));
    } catch (error) {
        message.channel.send(`Error: ${error}`);
    }
});

addCommand(false, {name: "claim"}, async (message, args) => {
    let role = commands.getRole(args.join(" "));
    if (role !== null) {
        if (config.roles_blacklist.includes(role.id)) {
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

addCommand(false, {name: "info"}, async (message, args) => {
    let claims = commands.getClaims(message.author);
    if (Object.keys(claims).length === 0)
        return message.channel.send("You currently have not claimed any roles.");
    let embed = new discord.RichEmbed()
        .setTitle(`${message.author.username} Claims Info`)
        .setColor("CYAN")
        .setThumbnail(message.author.avatarURL)
        .setDescription("Role, followed by remaining changes of each type.\n");
    for (let id of Object.keys(claims))
        embed.description += `<@&${id}> name:${claims[id].name} colour:${claims[id].colour}\n`;
    message.channel.send(embed);
});

addCommand(true, {name: "blacklist"}, async (message, args) => {
    let role;
    switch (args.shift().toLowerCase()) {
        case "add":
            role = commands.getRole(args.join(" "));
            if (role === null)
                return message.channel.send("No role found with that id.");
            if (config.roles_blacklist.includes(role.id))
                return message.channel.send(new discord.RichEmbed().setColor("ORANGE").setDescription(`${role} is already in the blacklist.`));
            config.roles_blacklist.push(role.id);
            commands.writeConfig();
            message.channel.send(new discord.RichEmbed().setColor("GREEN").setDescription(`${role} has been added to the blacklist.`));
            break;
        case "remove":
            role = commands.getRole(args.join(" "));
            if (role === null)
                message.channel.send("No role found with that id.");
            for (let i in config.roles_blacklist) {
                if (config.roles_blacklist[i] === role.id) {
                    config.roles_blacklist.splice(i, 1);
                    commands.writeConfig();
                    message.channel.send(new discord.RichEmbed().setColor("GREEN").setDescription(`${role} has been removed from the blacklist.`));
                    return;
                }
            }
            message.channel.send(new discord.RichEmbed().setColor("ORANGE").setDescription(`${role} was not in the blacklist.`));
            break;
        case "info":
            let i, j, chunk, chunkSize = 10;
            let pages = [];
            for (i = 0, j = config.roles_blacklist.length; i < j; i += chunkSize) {
                chunk = config.roles_blacklist.slice(i, i + chunkSize);
                let embed = new discord.RichEmbed()
                    .setTitle("Blacklist")
                    .setColor("BLACK")
                    .setDescription("")
                    .setFooter(`Page ${pages.length + 1} of ${Math.floor(config.roles_blacklist.length / chunkSize) + 1}`);
                for (let role of chunk)
                    embed.description += `<@&${role}> ${role}\n`;
                pages.push(embed);
            }
            let index = (parseInt(args[0]) || 1) - 1;
            let msg = await message.channel.send(pages[index]);
            commands.createPaginator(message, msg,
                () => {
                    index = ++index >= pages.length ? 0 : index;
                    msg.edit(pages[index]);
                },
                () => {
                    index = --index < 0 ? pages.length - 1 : index;
                    msg.edit(pages[index]);
                }
            );
            break;
        case "pregen":
            let start = config.roles_blacklist.length;
            for (let role of message.guild.roles.values())
                if (!config.roles_blacklist.includes(role.id))
                    if (role.members.size > 1)
                        config.roles_blacklist.push(role.id);
            commands.writeConfig();
            message.channel.send(new discord.RichEmbed().setColor("GREEN").setDescription(`Added ${config.roles_blacklist.length - start} values to the blacklist.`));
    }
});

addCommand(true, {name: "addchanges", pattern: /(?:give|add|grant)changes/}, async (message, args) => {
    let user = commands.getUser(message.guild, args[0]);
    if (user === null)
        return message.channel.send("Could not find the user specified.");
    let role = commands.getRole(args[1]);
    if (role === null)
        return message.channel.send("Could not find the role specified.");
    let amount = parseInt(args[3]);
    let claims = commands.getClaims(user);
    if (!claims[role.id])
        return message.channel.send("The user does not have a claim for that role.");
    if (!["both", "name", "colour"].includes(args[2].toLowerCase()))
        return message.channel.send("Please specify a proper value to add to (name,colour,both).");
    if (["name", "both"].includes(args[2].toLowerCase()))
        claims[role.id].colour += amount;
    if (["colour", "both"].includes(args[2].toLowerCase()))
        claims[role.id].name += amount;
    commands.writeClaims();
    message.channel.send(new discord.RichEmbed().setColor("GREEN").setDescription(`Added ${amount} to ${args[2]} changes for ${user}'s claim on ${role}.`));
});

addCommand(true, {name: "fixroles"}, async (message, args) => { //TODO
    let role = commands.getRole("lurker");
    let c = 0;
    for (let m of message.guild.members.values())
        if (!m.roles.has(role.id) && c++)
            m.addRole(role).catch(e => console.error(e));
    message.channel.send(`Repaired ${c} members.`);
});

addCommand(false, {name: "setname", pattern: /(?:re|set)name/}, async (message, args) => {
    let role = commands.getRole(args.shift());
    if (role === null)
        return message.channel.send("Unable to find the given role.");
    let claims = commands.getClaims(message.author);
    if (!claims[role.id])
        return message.channel.send("You do not have a claim for that role.");
    if (claims[role.id].name-- === 0)
        return message.channel.send("You have no remaining name changes for this claim.");
    await role.setName(args.join(" ")).catch(e => console.error(e));
    commands.writeClaims();
    message.channel.send(new discord.RichEmbed().setColor("GREEN").setDescription(`Successfully renamed ${role}. This role has ${claims[role.id].name} remaining name changes.`));
});

addCommand(false, {name: "setcolour", pattern: /(?:set|re)colou?r/}, async (message, args) => {
    let role = commands.getRole(args.shift());
    if (role === null)
        return message.channel.send("Unable to find the given role.");
    let claims = commands.getClaims(message.author);
    if (!claims[role.id])
        return message.channel.send("You do not have a claim for that role.");
    let colour = [parseInt(args.shift()), parseInt(args.shift()), parseInt(args.shift())];
    for (let c of config.colour_blacklist)
        if ((dist = Math.floor(commands.getColourDistance(c, colour))) < config.colour_distance_threshold)
            return message.channel.send(`The chosen colour is too close to ${util.inspect(c)} (distance ${dist}, minimum ${config.colour_distance_threshold}).`);
    if (claims[role.id].colour-- === 0)
        return message.channel.send("You have no remaining colour changes for this claim.");
    await role.setColor(colour).catch(e => console.error(e));
    commands.writeClaims();
    message.channel.send(new discord.RichEmbed().setColor("GREEN").setDescription(`Successfully changed ${role}'s colour. This role has ${claims[role.id].colour} remaining colour changes.`));
    // let dist = commands.getColourdistance()
});

addCommand(false, {name: "checkcolour", pattern: /checkcolou?r/}, async (message, args) => {
    let colour = [parseInt(args.shift()), parseInt(args.shift()), parseInt(args.shift())];
    let dist = 300;
    for (let c of config.colour_blacklist)
        if ((dist = Math.floor(commands.getColourDistance(c, colour))) < config.colour_distance_threshold)
            return message.channel.send(`The chosen colour is too close to ${util.inspect(c)} (distance ${dist}, minimum ${config.colour_distance_threshold}).`);
    message.channel.send(new discord.RichEmbed().setColor("GREEN").setDescription(`That colour is fine to use. (distance ${dist}, minimum ${config.colour_distance_threshold}).`));
});