job("kube1_groovy"){
  description("kubernetes job1")
  scm{
    github('Sumeet36/jenkins_kubernetes','master')
  }
  steps{
    shell('sudo cp -vrf * /home/jenkins')
  }
  triggers{
    gitHubPushTrigger()
  }
}

job("kube2_groovy"){
  steps{
    shell('''
	if sudo ls /home/jenkins | grep php
      	then
		if sudo kubectl get deployment --selector "app in (httpd)" | grep httpd-web
    		then
			sudo kubectl apply -f /home/jenkins/webserver.yml
           		POD=$(sudo kubectl get pod -l app=httpd -o jsonpath="{.items[0].metadata.name}")
        		echo $POD
        		sudo kubectl cp /home/jenkins/index.php $POD:/var/www/html
		else
    			if ! kubectl get pvc | grep httpdweb1-pv-claim
        		then
            			sudo kubectl create -f /home/jenkins/pvc.yml
        		fi
        		sudo kubectl create -f /home/jenkins/webserver.yml
        		POD=$(sudo kubectl get pod -l app=httpd -o jsonpath="{.items[0].metadata.name}")
        		echo $POD
        		sudo kubectl cp /home/jenkins/index.php $POD:/var/www/html
    		fi
   	fi
	''')
  }
  triggers {
        upstream('kube1_groovy', 'SUCCESS')
  }
}

job("kube3_groovy")
{
  steps{
    shell('''
status=$(curl -o /dev/null -s -w "%{http_code}" http://192.168.99.100:30002)
if [[ $status == 200 ]]
then
    echo "Running"
    exit 0
else
     exit 1
fi
     ''')
  }
  
  triggers {
        upstream('kube2_groovy', 'SUCCESS')
  }
  
  publishers {
        extendedEmail {
            recipientList('sumeetsumit8@gmail.com')
            defaultSubject('Job status')
          	attachBuildLog(attachBuildLog = true)
            defaultContent('Status Report')
            contentType('text/html')
            triggers {
                always {
                    subject('build Status')
                    content('Body')
                    sendTo {
                        developers()
                        recipientList()
                    }
		           }
	         }
     	 }
    }
}
