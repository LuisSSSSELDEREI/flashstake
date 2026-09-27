#!/bin/bash
set -e
cd /c/Users/Luiska/Desktop/TTsoft/upgrader_mod_work/forge_mdk
export PATH="/c/Program Files/Git/bin:$PATH"
git add -A
git status -sb
export GIT_AUTHOR_NAME="flashlight and luis"
export GIT_AUTHOR_EMAIL="flashlight-luis@users.noreply.github.com"
export GIT_COMMITTER_NAME="flashlight and luis"
export GIT_COMMITTER_EMAIL="flashlight-luis@users.noreply.github.com"
git commit -m "Initial commit: FlashStake 1.1.0 for Forge 1.20.1."
git log -1 --oneline
git status -sb
echo DONE
