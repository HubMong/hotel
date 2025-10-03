git fetch origin
git push -u origin developer --force-with-lease
cd C:\Users\ksy\Desktop\dawoo\playground\project\hotel
git init
git config user.name "ksy"    && git config user.email "ago0002006@naver.com"
git checkout -b developer
git add .
git commit -m "chore: push local as latest"
git remote add origin https://github.com/HubMong/hotel.git
git push -u origin devloper --force-with-lease
