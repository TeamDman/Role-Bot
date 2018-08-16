# Role-Bot

## Setup

### Installation

`git clone https://github.com/TeamDman/Role-Bot`

`cd Role-Bot`

`npm install`

### Running

`node app.js`

## Commands

#### /claims cmds
Lists the commands

#### /claims help
Displays basic information about claiming roles and managing them.

#### /claims restrictions
Displays basic information about claiming and managing restrictions.

#### /claims setname @role name
Renames the claimed role.

#### /claims setcolour @role r g b
Recolours the claimed role.

#### /claims inforaw
Displays the contents of `config.json`.

#### /claims setraw index value
Evaluates `config[index]=value`, allowing the config to be changed without manually opening it,.

#### /claims claim @role
Attempts to claim the specified role.

#### /claims info
Displays information about your claims.

#### /claims blacklist add @role
Adds a role to the blacklist.

#### /claims blacklist remove @role
Removes a role from the blacklist.

#### /claims blacklist info
Displays the contents of the blacklist.

#### /claims blacklist pregen
Adds to the blacklist any role with more than 1 member.

#### /claims listroles
Lists all roles and their id's.

#### /claims addchanges @user @role <name|colour|both> amount
Gives a user more changes of the specified type for the specified role.

#### /claims fixroles
Assigns the lurker role to any members that do not have it.

#### /claims checkcolour r g b
Displays whether or not the colour is valid, and it's distance from the banlist.
