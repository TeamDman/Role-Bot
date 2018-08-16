# Role-Bot

## Setup

### Installation

`git clone https://github.com/TeamDman/Role-Bot`

`cd Role-Bot`

`npm install`

### Running

`node app.js`

## Commands

#### USER /claims cmds
Lists the commands

#### USER /claims help
Displays basic information about claiming roles and managing them.

#### USER /claims restrictions
Displays basic information about claiming and managing restrictions.

#### USER /claims setname @role name
Renames the claimed role.

#### USER /claims setcolour @role r g b
Recolours the claimed role.

#### ADMIN /claims inforaw
Displays the contents of `config.json`.

#### ADMIN /claims setraw index value
Evaluates `config[index]=value`, allowing the config to be changed without manually opening it,.

#### USER /claims claim @role
Attempts to claim the specified role.

#### USER /claims info
Displays information about your claims.

#### ADMIN /claims blacklist add @role
Adds a role to the blacklist.

#### ADMIN /claims blacklist remove @role
Removes a role from the blacklist.

#### ADMIN /claims blacklist info
Displays the contents of the blacklist.

#### ADMIN /claims blacklist pregen
Adds to the blacklist any role with more than 1 member.

#### ADMIN /claims addchanges @user @role <name|colour|both> amount
Gives a user more changes of the specified type for the specified role.

#### ADMIN /claims fixroles
Assigns the lurker role to any members that do not have it.

#### USER /claims checkcolour r g b
Displays whether or not the colour is valid, and it's distance from the banlist.
