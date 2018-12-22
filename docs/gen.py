import os

def warn(str):
    print('\033[91m[WARN] ' + str + '\033[0m')

def success(str):
    print('\033[92m[OK]   ' + str + '\033[0m')

print('\n=== docs/gen.py: GENERATING DOCUMENTATION ===\n')

if os.path.isfile('gen_classes.py'):
    import gen_classes
    success('generated docs/CLASSES.md')
else:
    warn('could not generate CLASSES.md because gen_classes.py was not found (it is not included in the repository because of license issues)')
print('\n=============================================\n')