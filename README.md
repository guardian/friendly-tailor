# friendly-tailor



On your local dev box you should have:

```
$ cat /etc/gu/friendly-tailor.stage.conf
stage=DEV

```

## Running

```
sbt run
```

Then hit http://localhost:9000/healthcheck until it reports a content update time.
