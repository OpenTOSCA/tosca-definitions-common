#!/bin/bash
python3 -m pip install click==7.1.2 # otherwise conflict with the plugin runner
python3 -m pip install click-didyoumean==0.0.3 # https://pypi.org/project/click-didyoumean/#history they actually broke everthing in exactly one hour we were testing
python3 -m pip install gunicorn==20.1.0