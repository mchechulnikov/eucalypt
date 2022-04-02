.DEFAULT_GOAL = help

help:	# prints this help
	@ echo Available targets are:
	@ grep -h -E "^[^\#].+:\s+\#\s+.+$$" ./*.makefile ./makefile

