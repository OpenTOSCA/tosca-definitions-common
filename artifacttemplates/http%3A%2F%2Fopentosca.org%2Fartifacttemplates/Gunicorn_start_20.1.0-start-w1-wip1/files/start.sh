#!/bin/bash
python -m gunicorn --daemon --workers 4 --bind 0.0.0.0:$Port "$AppModule"
