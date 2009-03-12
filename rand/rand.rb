#!/usr/bin/ruby

numbers = []

Dir["rand/rand*.dat"].each{|filename|
	File.open(filename, "r") {|file|
		while line = file.gets do
			line = line.scan(/[a-fA-F0-9]{4}/)
			while line.size >= 2 do
				a, b, *line = line
				numbers.push("0x"+a+b)
			end
		end
	}
}

while line = gets do
	if line =~ /PUT CODE HERE/
		ii = 0;
		kk = 1;
		while ii < numbers.size do
			print "\tstatic final void initialize_tab_#{kk}() {\n"
			(ii...[(ii+5000),numbers.size].min).each {|ii|
				print "\t\ttab[#{ii}] = #{numbers[ii]};\n"
			}
			ii += 1
			kk += 1
			print "\t}\n"
		end
		print "\tstatic {\n"
		print "\t\ttab = new int[#{numbers.size}];\n"
		(1...kk).each{|kk| print "\t\tinitialize_tab_#{kk}();\n" }
		print "\t}\n"
	elsif line =~ /\/\*TAB SIZE\*\//
		print line.gsub(/\/\*TAB SIZE\*\//, numbers.size.to_s)
	else print line
	end
end

