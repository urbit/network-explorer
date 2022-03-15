curl --cookie "urbauth-~zod=0v6.acn9b.ibgvn.jjrug.pn8iu.lvnuv" http://localhost:8080/~/scry/gaze/raw.txt | aws s3 cp - s3://gaze-exports/events.txt --acl public-read --content-type 'text/plain'
