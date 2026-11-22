"""Run after ./build.sh. Uses only the Python standard library."""
from pathlib import Path
from subprocess import run
from tempfile import TemporaryDirectory

root = Path(__file__).resolve().parents[1]


def invoke(*args):
    return run([root / "run.sh", *args], capture_output=True, text=True)


with TemporaryDirectory() as directory:
    source = Path(directory) / "example.decaf"
    output = Path(directory) / "tokens.txt"
    source.write_bytes(b"/* a\r\nb */ int_x += 0xFeL; // comment\rtrue")
    result = invoke("--target", "scan", "--output", output, source)
    assert result.returncode == 0 and not result.stdout, result.stderr
    assert output.read_text().splitlines() == [
        "2 IDENTIFIER int_x", "2 +=", "2 LONGLITERAL 0xFeL", "2 ;", "3 BOOLEANLITERAL true"
    ]

    result = invoke(root / "tests/sum.decaf")
    assert result.returncode == 0 and not result.stdout, result.stderr
    source.write_text("int f() { return 8 - 3 - 2 * 4; }")
    result = invoke("-d", source)
    assert result.returncode == 0, result.stderr
    assert result.stdout.splitlines() == [
        "program", "  method int f", "    parameters", "    block", "      return",
        "        -", "          -", "            8", "            3", "          *",
        "            2", "            4"
    ]

    for invalid in [
        "void f() {} int late;", "int a[2L];", "void f(){return 1}",
        "void f(){g(); int late;}", 'void f(){return "text";}',
        "void f(){for(a[1]=0;true;a++) {}}", "void f(){if(true) return;}",
        "/* unfinished", "int x;\n@", "void f(){x='ab';}", r'void f(){g("\q");}'
    ]:
        source.write_text(invalid)
        result = invoke(source)
        assert result.returncode != 0 and str(source) + ":" in result.stderr, invalid

print("OK")
