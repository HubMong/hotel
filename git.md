브랜치 생성 → 푸시

# (1) 현재 커밋 기준 새 브랜치 생성
git switch -c developer
#  또는: git checkout -b developer

# (2) 원격에 올리고 추적 설정
git push -u origin developer

# 다른 브랜치를 기준으로 자르고 싶다면 (예: my-base에서)
git switch -c developer my-base
git push -u origin developer

브랜치 삭제 (로컬/원격)
# (A) 로컬 삭제git fetch hotel --prune

# 먼저 해당 브랜치 위에 있으면 삭제 불가 → 다른 브랜치로 이동
git switch main        # 또는 git checkout --detach
git branch -d developer   # 병합된 경우만 삭제
git branch -D developer   # 병합 안 됐어도 강제 삭제

# (B) 원격 삭제
git push origin --delete developer
# (동일) git push origin :developer