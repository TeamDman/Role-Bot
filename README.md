# Role-Bot
A java based discord bot to manage custom user roles.

Users may claim roles with the `/claims claim <id>` command.
From there, they can use either `/claims renameclaim <id> <name>` or `/claims recolourclaim <id> <r> <g> <b>`


# Setup
Running `java -jar role-bot-version-all.jar` will load the bot.
It will generate a `bot.properties` file, where you can configure some aspects of the bot.
 - DISCORD_TOKEN: The token of the bot
 - COOLDOWN: The time in seconds users have to wait before modifying their role again
 
### Commands
Commands have 2 tiers, ADMIN and USER tier.

#### USER /claims help
Displays a basic list of commands and the arguments they use.

#### USER /claims info
Displays information about the claim system.

#### USER /claims listroles
Displays all roles in the guild, with their IDs.

#### USER /claims claim roleid
Attempts to claim the specific role id.
Roles in the blacklist are unclaimable.
Roles you already have a claim for are unclaimable.
Roles you do not have are unclaimable.

#### USER /claims listclaims
Lists all claims with associated users, and the last time the claim was updated.

#### USER /claims recolourclaim roleid r g b
Attempts to set the colour of a claimed role.
This command is limited by the cooldown.

#### USER /claims renameclaim roleid name
Attempts to rename a claimed role.
This command is limited by the cooldown.

#### ADMIN /claims removeclaims roleid
Removes all claims attached to the specific role id.

#### ADMIN /claims forceclaim userid roleid
Forces a claim on the given role by the given user.
This ignores any blacklist restrictions.

#### USER /claims checkcolour r g b
Displays information about the colour's distance from those in the blacklist.

#### ADMIN /claims resetcooldown userid
Resets the cooldown on all claims made by the given user.

#### ADMIN /claims grantchanges userid roleid amount
Grants the user name and colour changes for the specified role.

#### USER /claims listblacklist
Displays all roles in the blacklist, with their IDs.

#### ADMIN /claims blacklistadd roleid
Adds the provided role id to the blacklist.

#### ADMIN /claims blacklistremove roleid
Removes the provided role id from the blacklist.

#### ADMIN /claims pregenblacklist
Adds all roles with more than 1 user to the blacklist.

#### USER /claims listunused
Lists all roles with 0 users.

#### ADMIN /claims setglobalcooldown seconds
Sets the global cooldown in seconds to the given value.

#### ADMIN /claims setglobalchangelimit amount
Sets the maximum amount of times users are allowed to change their names and colours.


## Notes
The bot still sometimes throws an error related to webhooks when it starts. To my knowledge, this can be ignored.
The the cooldown and token values are stored in `bot.properties`.
The blacklist is stored in `blacklist.txt`, IDs are separated by newlines.
The claims are stored in `claims.txt`, in the format of `user`, `role`, `timestamp`, entriesseparated by newlines.


