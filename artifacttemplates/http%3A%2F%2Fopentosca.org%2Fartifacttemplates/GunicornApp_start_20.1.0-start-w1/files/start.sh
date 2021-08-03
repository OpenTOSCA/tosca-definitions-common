#!/bin/bash
python3 -m gunicorn --daemon --workers 4 --bind 0.0.0.0:$Port "$AppModule"
