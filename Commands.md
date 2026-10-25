Собрать проект:
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1

Вывести токены программы:
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 .\test.dcf -t scan

Проверить синтаксис:
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 .\test.dcf -t parse
$LASTEXITCODE


Сохранить список токенов в файл:
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 .\test.dcf -t scan -o .\tokens.txt

Вывести краткую отладочную информацию:
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 .\test.dcf -t parse -d

Коды завершения

0 - анализ выполнен успешно
1 - лексическая или синтаксическая ошибка во входной программе
2 - неверные параметры запуска или ошибка чтения/записи файла

Проверить сканер на примере из задания, ожидается код 0:
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 .\tests\scan_ok.decaf -t scan

Проверить разные токены и комментарии, ожидается код 0:
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 .\tests\scan_all.decaf -t scan

Проверить лексические ошибки, ожидаются три сообщения и код 1:
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 .\tests\scan_error.decaf -t scan

Проверить правильный синтаксис, ожидается код 0 без вывода:
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 .\tests\parse_ok.decaf -t parse

Проверить синтаксическую ошибку, ожидается код 1:
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 .\tests\parse_error.decaf -t parse

$LASTEXITCODE
