"""核對 CLAUDE.md 所列的 coding 格式。

檢查 app/src/main/java/ 底下每個 Kotlin 檔：
  1. companion object 在宣告區塊最前面
  2. 變數依字母排序（大小寫不敏感）
  3. 函式依字母排序（大小寫不敏感）
  4. 每個函式與常數上方都有註解

排序的範圍是同一個宣告區塊，三種 scope 分開看：
  - 宣告區塊主體（class / object / interface / companion 的 {}）
  - 主建構子的參數列（data class 的 val/var）
  - 函式主體（區域變數，不受規則約束，忽略）

用法：python tools/check_style.py
符合回傳 0，有問題回傳 1。
"""
import glob
import io
import re
import sys

SOURCES = 'app/src/main/java/**/*.kt'

BACKSLASH = chr(92)
ANNOTATION = re.compile(r'^@\w+')
CONST = re.compile(r'^(?:private )?const val\s+(\w+)')
DECL = re.compile(
    r'^\s*(?:@\w+\s+)*(?:public |internal |private |protected )?'
    r'(?:abstract |open |sealed |data |enum |inline |value )*'
    r'(class|object|interface|companion object)\b\s*(\w+)?'
)
FUN = re.compile(
    r'^(?:@\w+\s+)*(?:override |private |internal |public |inline |suspend |operator )*'
    r'fun\s+(?:<[^>]+>\s+)?(?:[\w.]+\.)?(\w+)\s*[(<]'
)
PROP = re.compile(
    r'^(?:@\w+\s+)*(?:override |private |internal |lateinit |const )*(?:val|var)\s+(\w+)\s*[:=]'
)


def strip_noise(line):
    """去掉字串字面值與行註解，避免裡面的括號被當成區塊。"""
    out = []
    i = 0
    quote = None
    while i < len(line):
        c = line[i]
        if quote:
            if c == BACKSLASH:
                i += 2
                continue
            if c == quote:
                quote = None
            i += 1
            continue
        if c == '"':
            quote = '"'
            i += 1
            continue
        if line.startswith('//', i):
            break
        out.append(c)
        i += 1
    return ''.join(out)


def new_scope():
    return {'fun': [], 'prop': [], 'companion_at': None, 'members': 0}


def check_comments(lines):
    """規則 4：每個成員函式與常數上方都要有註解。"""
    def has_comment_above(index):
        i = index - 1
        while i >= 0:
            s = lines[i].strip()
            if not s or ANNOTATION.match(s):
                i -= 1
                continue
            return s.endswith('*/') or s.startswith('//') or s.startswith('*')
        return False

    problems = []
    for n, raw in enumerate(lines):
        s = raw.strip()
        m = FUN.match(s)
        if m and not raw.startswith('        '):
            if not has_comment_above(n):
                problems.append('fun %s (第 %d 行) 上方缺註解' % (m.group(1), n + 1))
            continue
        m = CONST.match(s)
        if m and not has_comment_above(n):
            problems.append('const %s (第 %d 行) 上方缺註解' % (m.group(1), n + 1))
    return problems


def check_order(lines):
    """規則 1-3：companion 位置與字母排序。"""
    stack = []
    scopes = {}
    pending = None
    ctor_of = None
    parens = 0
    in_comment = False

    for raw in lines:
        stripped = raw.strip()
        if in_comment:
            if '*/' in stripped:
                in_comment = False
            continue
        if stripped.startswith('/*'):
            if '*/' not in stripped:
                in_comment = True
            continue

        clean = strip_noise(raw)
        body = clean.strip()

        decl = DECL.match(raw)
        if decl:
            if decl.group(1) == 'companion object':
                scope = scopes.setdefault(tuple(stack), new_scope())
                scope['companion_at'] = scope['members']
            pending = decl.group(2) or 'Companion'
        elif body and parens == 0:
            if (not stack) or stack[-1] != '<blk>':
                scope = scopes.setdefault(tuple(stack), new_scope())
                m = FUN.match(body)
                if m:
                    scope['fun'].append(m.group(1))
                    scope['members'] += 1
                else:
                    m = PROP.match(body)
                    if m:
                        scope['prop'].append(m.group(1))
                        scope['members'] += 1
        elif body and parens > 0 and ctor_of:
            m = PROP.match(body)
            if m:
                scopes.setdefault(('ctor', ctor_of), new_scope())['prop'].append(m.group(1))

        for c in clean:
            if c == '(':
                if parens == 0 and pending:
                    ctor_of = pending
                parens += 1
            elif c == ')':
                parens -= 1
                if parens == 0:
                    ctor_of = None
            elif c == '{':
                stack.append(pending or '<blk>')
                pending = None
            elif c == '}':
                if stack:
                    stack.pop()
        # 宣告沒有 {} 主體（例如 data object X : Y）就不要把 pending 留給下一個區塊。
        # 但多行的類別標頭（class Foo( 換行 ) {）括號還沒平衡，那時 pending 必須留著，
        # 否則主體會被當成匿名區塊而整個跳過檢查。
        if decl and '{' not in clean and parens == 0:
            pending = None

    problems = []
    for key, scope in scopes.items():
        if key and key[0] == 'ctor':
            where = key[1] + '(建構子)'
        elif key and key[-1] == '<blk>':
            continue
        else:
            where = 'top-level' if not key else '::'.join(key)
        for label in ('prop', 'fun'):
            names = scope[label]
            want = sorted(names, key=str.lower)
            if names != want:
                problems.append('[%s] %s 排序錯誤' % (where, label))
                problems.append('    實際 %s' % names)
                problems.append('    應為 %s' % want)
        if scope['companion_at'] not in (None, 0):
            problems.append(
                '[%s] companion object 前面還有 %d 個成員' % (where, scope['companion_at'])
            )
    return problems


def main():
    paths = sorted(glob.glob(SOURCES, recursive=True))
    if not paths:
        print('找不到原始碼，請從專案根目錄執行')
        return 1

    total = 0
    for path in paths:
        lines = io.open(path, encoding='utf-8').read().split('\n')
        problems = check_order(lines) + check_comments(lines)
        print(('OK  ' if not problems else 'XX  ') + path)
        for line in problems:
            print('    ' + line)
        total += len(problems)

    print('')
    if total:
        print('%d 處不符合 CLAUDE.md 的 coding 格式' % total)
        return 1
    print('%d 個檔案全部符合' % len(paths))
    return 0


if __name__ == '__main__':
    sys.exit(main())
